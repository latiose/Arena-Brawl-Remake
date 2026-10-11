package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.ultimate.Rewind;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class PlayerHealthManager {

    public record DamageEvent(Player attacker, Player victim, double amount) {}
    public record HealingEvent(Player healer, Player target, double amount) {}

    private static final double VANILLA_MAX = 20.0;
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Double> currentHealth = new HashMap<>();
    private final Map<UUID, Double> maxHealth = new HashMap<>();
    private record HealReduction(double percent, long durationMillis) {}
    private final Map<UUID, List<HealReduction>> healReductions = new HashMap<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private static final long HOLOGRAM_LIFETIME_TICKS = 35;
    private Consumer<Player> eliminationCallback = p -> {};
    private Consumer<DamageEvent> damageCallback = event -> {};
    private Consumer<HealingEvent> healingCallback = event -> {};
    private AbilityConfig config;
    public void setEliminationCallback(Consumer<Player> callback) {
        this.eliminationCallback = callback;
    }

    public void setDamageCallback(Consumer<DamageEvent> callback) {
        this.damageCallback = callback;
    }

    public void setHealingCallback(Consumer<HealingEvent> callback) {
        this.healingCallback = callback;
    }

    public PlayerHealthManager(AbilityConfig config) {
        this.config = config;
    }
    public void setMaxHealth(Player player, double max) {
        maxHealth.put(player.getUniqueId(), max);
        currentHealth.put(player.getUniqueId(), max);
        eliminated.remove(player.getUniqueId());
        syncVanilla(player);
    }

    public double getMaxHealth(Player player) {
        return maxHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public double getHealth(Player player) {
        return currentHealth.getOrDefault(player.getUniqueId(), 2000.0);
    }

    public boolean canRegen(Player player) {
        return getHealingMultiplier(player) > 0.0;
    }

    public boolean isRegenDisabled(Player player) {
        return getHealingMultiplier(player) <= 0.0;
    }

    public void disableRegen(Player player, long durationMillis) {
        addHealReduction(player, 100.0, durationMillis);
    }

    public void addHealReduction(Player player, double percent, long durationMillis) {
        healReductions.computeIfAbsent(player.getUniqueId(), ignored -> new java.util.ArrayList<>())
                .add(new HealReduction(Math.max(0.0, percent), durationMillis));
    }

    public void removeHealReduction(Player player, double percent, long durationMillis) {
        List<HealReduction> reductions = healReductions.get(player.getUniqueId());
        if (reductions == null) return;

        for (int i = 0; i < reductions.size(); i++) {
            HealReduction reduction = reductions.get(i);
            if (Double.compare(reduction.percent(), Math.max(0.0, percent)) == 0
                    && reduction.durationMillis() == durationMillis) {
                reductions.remove(i);
                break;
            }
        }

        if (reductions.isEmpty()) {
            healReductions.remove(player.getUniqueId());
        }
    }

    public double getHealingMultiplier(Player player) {
        List<HealReduction> reductions = healReductions.get(player.getUniqueId());
        if (reductions == null) return 1.0;

        double reduction = reductions.stream()
                .mapToDouble(HealReduction::percent)
                .sum();
        return Math.max(0.0, 1.0 - Math.min(100.0, reduction) / 100.0);
    }

    public double heal(Player player, double amount) {
        return heal(player, amount, true);
    }

    private double heal(Player player, double amount, boolean recordHealing) {
        if (eliminated.contains(player.getUniqueId())) return 0.0;

        double effectiveAmount = amount * getHealingMultiplier(player);
        if (effectiveAmount <= 0.0) return 0.0;

        double max = getMaxHealth(player);
        double healedAmount = Math.min(effectiveAmount, max - getHealth(player));
        if (healedAmount <= 0.0) return 0.0;

        double updated = getHealth(player) + healedAmount;
        currentHealth.put(player.getUniqueId(), updated);
        if (recordHealing) {
            healingCallback.accept(new HealingEvent(player, player, healedAmount));
        }

        int roundedHeal = (int) Math.round(healedAmount);
        spawnHologram(player, String.valueOf(roundedHeal), player.getLocation());
        syncVanilla(player);
        return healedAmount;
    }

    public double heal(Player player, double amount, String sourceName) {
        double healedAmount = heal(player, amount);
        int roundedHeal = (int) Math.round(healedAmount);

        player.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s healed you for §a%d §3health!",
                sourceName, roundedHeal
        ));
        return healedAmount;
    }

    public void healAlly(Player healer, Player target, double amount, String sourceName) {
        if (eliminated.contains(target.getUniqueId())) return;

        double healedAmount = heal(target, amount, false);
        healingCallback.accept(new HealingEvent(healer, target, healedAmount));
        int roundedHeal = (int) Math.round(healedAmount);

        healer.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s healed %s §3for §a%d §3health!",
                sourceName, target.getName(), roundedHeal
        ));

        target.sendMessage(MessageUtils.positive() + String.format(
                "§3%s§3's %s healed you for §a%d §3health!",
                healer.getName(), sourceName, roundedHeal
        ));
    }

    public void damage(Player player, double amount, Player attacker) {
        if (eliminated.contains(player.getUniqueId())) return;

        if (attacker != null) {
            lastAttacker.put(player.getUniqueId(), attacker.getUniqueId());
        }

        double updated = Math.max(getHealth(player) - amount, 0);
        double damageDealt = Math.min(amount, getHealth(player));
        currentHealth.put(player.getUniqueId(), updated);
        if (attacker != null && damageDealt > 0) {
            damageCallback.accept(new DamageEvent(attacker, player, damageDealt));
        }
        syncVanilla(player);

        if (updated <= 0) {
            UUID casterUUID = Rewind.ACTIVE_REWUNDS.remove(player.getUniqueId());

            if (casterUUID != null) {
                currentHealth.put(player.getUniqueId(),  config.getDouble("revive-health", 400.0));

                Location loc = player.getLocation();

                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
                loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);

                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 100, 0.5, 0.8, 0.5, 0.3);
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 50, 0.5, 1.0, 0.5, 0.1);

                Player caster = Bukkit.getPlayer(casterUUID);
                if (caster != null && caster.isOnline()) {
                    caster.sendMessage(MessageUtils.positive() + String.format("§eYour Rewind revived %s with §a400 HP§3!", player.getName()));
                }
                player.sendMessage(MessageUtils.positive() + "§3Rewind saved you from death! Restored §a400 HP§3!");
                return;
            }
        }
        if (updated <= 0) {
            eliminated.add(player.getUniqueId());
            eliminationCallback.accept(player);
        }
    }

    public boolean isEliminated(Player player) {
        return eliminated.contains(player.getUniqueId());
    }

    private void syncVanilla(Player player) {
        double percentage = getHealth(player) / getMaxHealth(player);
        double vanillaHealth = Math.max(0.5, percentage * VANILLA_MAX);

        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && attribute.getBaseValue() != VANILLA_MAX) {
            attribute.setBaseValue(VANILLA_MAX);
        }

        player.setHealth(Math.min(vanillaHealth, VANILLA_MAX));
    }

    private void syncVanillaSilent(Player player) {
        double percentage = getHealth(player) / getMaxHealth(player);
        double vanillaHealth = Math.max(0.5, percentage * VANILLA_MAX);

        var attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null && attribute.getBaseValue() != VANILLA_MAX) {
            attribute.setBaseValue(VANILLA_MAX);
        }
        player.setHealth(Math.min(vanillaHealth, VANILLA_MAX));
    }

    public void damage(Player player, double amount) {
        damage(player, amount, null);
    }

    public UUID getLastAttacker(Player player) {
        return lastAttacker.get(player.getUniqueId());
    }

    public void damageSilent(Player player, double amount, Player attacker) {
        if (eliminated.contains(player.getUniqueId())) return;

        if (attacker != null) {
            lastAttacker.put(player.getUniqueId(), attacker.getUniqueId());
        }

        double updated = Math.max(getHealth(player) - amount, 0);
        double damageDealt = Math.min(amount, getHealth(player));
        currentHealth.put(player.getUniqueId(), updated);
        if (attacker != null && damageDealt > 0) {
            damageCallback.accept(new DamageEvent(attacker, player, damageDealt));
        }
        syncVanillaSilent(player);
        if (updated <= 0) {
            UUID casterUUID = Rewind.ACTIVE_REWUNDS.remove(player.getUniqueId());

            if (casterUUID != null) {
                currentHealth.put(player.getUniqueId(), config.getDouble("revive-health", 400.0));

                Location loc = player.getLocation();

                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
                loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.5f);

                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 100, 0.5, 0.8, 0.5, 0.3);
                loc.getWorld().spawnParticle(Particle.REVERSE_PORTAL, loc, 50, 0.5, 1.0, 0.5, 0.1);

                Player caster = Bukkit.getPlayer(casterUUID);
                if (caster != null && caster.isOnline()) {
                    syncVanillaSilent(player);
                    caster.sendMessage(MessageUtils.positive() + String.format("§eYour Rewind revived %s with §a400 HP§3!", player.getName()));
                }
                player.sendMessage(MessageUtils.positive() + "§3Rewind saved you from death! Restored §a400 HP§3!");
                return;
            }
        }
        if (updated <= 0) {
            eliminated.add(player.getUniqueId());
            eliminationCallback.accept(player);
        }
    }

    public void damageSilent(Player player, double amount) {
        damageSilent(player, amount, null);
    }

    public void clearRegenDisable(Player player) {
        healReductions.remove(player.getUniqueId());
    }

    private void spawnHologram(Player victim, String phrase, Location impactLocation) {
        float jitterX = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);
        float jitterY = (float) ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
        float jitterZ = (float) ThreadLocalRandom.current().nextDouble(-0.15, 0.15);

        Location spawnLoc = impactLocation.clone().add(0, 1.0, 0);
        TextColor color = TextColor.color(0x90EE90);

        TextDisplay hologram = victim.getWorld().spawn(spawnLoc, TextDisplay.class, d -> {
            d.text(Component.text(phrase, color));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(100, 10, 50, 20));

            Transformation currentTransform = d.getTransformation();
            Vector3f translation = new Vector3f(jitterX, jitterY, jitterZ);

            d.setTransformation(new Transformation(
                    translation,
                    currentTransform.getLeftRotation(),
                    currentTransform.getScale(),
                    currentTransform.getRightRotation()
            ));
        });
        EntityCleanupUtils.markAsArenaEntity(hologram);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (hologram.isDead()) {
                    cancel();
                    return;
                }

                if (ticksElapsed >= HOLOGRAM_LIFETIME_TICKS) {
                    hologram.remove();
                    cancel();
                    return;
                }
                hologram.teleport(hologram.getLocation().add(0, 0.02, 0));
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }
}