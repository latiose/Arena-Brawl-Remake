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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class PlayerHealthManager {

    private static final double VANILLA_MAX = 20.0;
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Double> currentHealth = new HashMap<>();
    private final Map<UUID, Double> maxHealth = new HashMap<>();
    private final Map<UUID, Long> regenDisabledUntil = new HashMap<>();
    private final Set<UUID> eliminated = new HashSet<>();
    private static final long HOLOGRAM_LIFETIME_TICKS = 35;
    private Consumer<Player> eliminationCallback = p -> {};
    private AbilityConfig config;
    public void setEliminationCallback(Consumer<Player> callback) {
        this.eliminationCallback = callback;
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
        return !isRegenDisabled(player);
    }

    public boolean isRegenDisabled(Player player) {
        Long disabledUntil = regenDisabledUntil.get(player.getUniqueId());
        if (disabledUntil == null) return false;
        return System.currentTimeMillis() < disabledUntil;
    }

    public void disableRegen(Player player, long durationMillis) {
        regenDisabledUntil.put(player.getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    public void heal(Player player, double amount) {
        if (eliminated.contains(player.getUniqueId())) return;
        if (!canRegen(player)) return;

        double max = getMaxHealth(player);
        double updated = Math.min(getHealth(player) + amount, max);
        currentHealth.put(player.getUniqueId(), updated);

        int roundedHeal = (int) Math.round(amount);
        spawnHologram(player, String.valueOf(roundedHeal), player.getLocation());
        syncVanilla(player);
    }

    public void heal(Player player, double amount, String sourceName) {
        if (eliminated.contains(player.getUniqueId())) return;
        if (!canRegen(player)) return;

        heal(player, amount);
        int roundedHeal = (int) Math.round(amount);

        player.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s healed you for §a%d §3health!",
                sourceName, roundedHeal
        ));
    }

    public void healAlly(Player healer, Player target, double amount, String sourceName) {
        if (eliminated.contains(target.getUniqueId())) return;
        if (!canRegen(target)) return;

        heal(target, amount);
        int roundedHeal = (int) Math.round(amount);

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
        currentHealth.put(player.getUniqueId(), updated);
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
        currentHealth.put(player.getUniqueId(), updated);
        syncVanilla(player);
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
                    syncVanilla(player);
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
        regenDisabledUntil.remove(player.getUniqueId());
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