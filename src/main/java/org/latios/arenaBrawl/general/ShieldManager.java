package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShieldManager {

    public record ActiveShield(double reductionPercent, long startedAt, long durationMillis, String title,
                               boolean notifyOnExpire, boolean pulsingParticles) {
        public boolean isExpired() {
            return System.currentTimeMillis() - startedAt >= durationMillis;
        }

        public long getRemainingMillis() {
            return Math.max(0, durationMillis - (System.currentTimeMillis() - startedAt));
        }
    }

    private final Map<UUID, List<ActiveShield>> activeShields = new HashMap<>();
    private final Map<UUID, Long> lastParticleAt = new HashMap<>();

    public void applyShield(Player player, double reductionPercent, long durationMillis, String title) {
        applyShield(player, reductionPercent, durationMillis, title, true, false);
    }

    /** Applies a shield that does not send an expiry message in chat. */
    public void applySilentShield(Player player, double reductionPercent, long durationMillis, String title) {
        applyShield(player, reductionPercent, durationMillis, title, false, false);
    }

    /** Applies a silent shield that emits a particle pulse every second while it lasts. */
    public void applySilentPulsingShield(Player player, double reductionPercent, long durationMillis, String title) {
        applyShield(player, reductionPercent, durationMillis, title, false, true);
    }

    private void applyShield(Player player, double reductionPercent, long durationMillis, String title,
                             boolean notifyOnExpire, boolean pulsingParticles) {
        if (pulsingParticles) {
            lastParticleAt.putIfAbsent(player.getUniqueId(), System.currentTimeMillis());
        }

        activeShields.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveShield(reductionPercent, System.currentTimeMillis(), durationMillis, title,
                        notifyOnExpire, pulsingParticles));
    }
    public void tick() {
        if (activeShields.isEmpty()) return;

        Iterator<Map.Entry<UUID, List<ActiveShield>>> mapIterator = activeShields.entrySet().iterator();

        while (mapIterator.hasNext()) {
            Map.Entry<UUID, List<ActiveShield>> entry = mapIterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            List<ActiveShield> shields = entry.getValue();

            List<ActiveShield> expiredShields = new ArrayList<>();
            for (ActiveShield shield : shields) {
                if (shield.isExpired()) {
                    expiredShields.add(shield);
                }
            }

            if (!expiredShields.isEmpty()) {
                shields.removeAll(expiredShields);
                if (player != null && player.isOnline()) {
                    for (ActiveShield expired : expiredShields) {
                        if (!expired.notifyOnExpire()) continue;
                        Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                                .append(Component.text(expired.title().toUpperCase(), NamedTextColor.AQUA, TextDecoration.BOLD))
                                .append(Component.text(" has expired!", NamedTextColor.YELLOW));
                        player.sendMessage(expireMessage);
                    }
                }
            }

            if (shields.isEmpty()) {
                mapIterator.remove();
                lastParticleAt.remove(entry.getKey());
                continue;
            }

            boolean hasPulsingShield = false;
            for (ActiveShield shield : shields) {
                if (shield.pulsingParticles()) {
                    hasPulsingShield = true;
                    break;
                }
            }

            if (hasPulsingShield) {
                long now = System.currentTimeMillis();
                Long lastPulse = lastParticleAt.get(entry.getKey());
                if (lastPulse == null || now - lastPulse >= 1000L) {
                    lastParticleAt.put(entry.getKey(), now);
                    if (player != null && player.isOnline()) {
                        spawnShieldPulse(player);
                    }
                }
            } else {
                lastParticleAt.remove(entry.getKey());
            }

            if (player != null && player.isOnline()) {
                ActiveShield highestShield = null;
                for (ActiveShield shield : shields) {
                    if (highestShield == null || shield.reductionPercent() > highestShield.reductionPercent()) {
                        highestShield = shield;
                    }
                }

                if (highestShield != null) {
                    double progress = (double) highestShield.getRemainingMillis() / highestShield.durationMillis();
                    StatusBarUtil.sendStatusBar(player, highestShield.title(), progress, "#00FFFF");
                }
            }
        }
    }

    public double getDamageReduction(Player player) {
        List<ActiveShield> shields = activeShields.get(player.getUniqueId());
        if (shields == null || shields.isEmpty()) return 0.0;

        shields.removeIf(ActiveShield::isExpired);

        if (shields.isEmpty()) {
            activeShields.remove(player.getUniqueId());
            return 0.0;
        }

        ActiveShield highestShield = null;
        for (ActiveShield shield : shields) {
            if (highestShield == null || shield.reductionPercent() > highestShield.reductionPercent()) {
                highestShield = shield;
            }
        }

        if (highestShield != null) {
            double progress = (double) highestShield.getRemainingMillis() / highestShield.durationMillis();
            StatusBarUtil.sendStatusBar(player, highestShield.title(), progress, "#00FFFF");
            return highestShield.reductionPercent();
        }

        return 0.0;
    }

    public double getReductionByTitle(Player player, String title) {
        List<ActiveShield> shields = activeShields.get(player.getUniqueId());
        if (shields == null || shields.isEmpty()) return 0.0;

        ActiveShield highestShield = null;
        for (ActiveShield shield : shields) {
            if (!shield.isExpired() && shield.title().equalsIgnoreCase(title)) {
                if (highestShield == null || shield.reductionPercent() > highestShield.reductionPercent()) {
                    highestShield = shield;
                }
            }
        }

        if (highestShield != null) {
            double progress = (double) highestShield.getRemainingMillis() / highestShield.durationMillis();
            StatusBarUtil.sendStatusBar(player, highestShield.title(), progress, "#00FFFF");
            return highestShield.reductionPercent();
        }

        return 0.0;
    }

    public void removeByTitle(Player player, String title) {
        List<ActiveShield> shields = activeShields.get(player.getUniqueId());
        if (shields == null) return;

        shields.removeIf(shield -> shield.title().equals(title));
        if (shields.isEmpty()) {
            activeShields.remove(player.getUniqueId());
            lastParticleAt.remove(player.getUniqueId());
        }
    }

    public void clear(Player player) {
        activeShields.remove(player.getUniqueId());
        lastParticleAt.remove(player.getUniqueId());
    }

    /** Dark particle ring emitted once per second by pulsing shields. */
    private void spawnShieldPulse(Player player) {
        Location center = player.getLocation().add(0, 1.0, 0);
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < 12; i++) {
            double angle = (Math.PI * 2.0 / 12) * i;
            Location point = center.clone().add(Math.cos(angle) * 0.8, 0, Math.sin(angle) * 0.8);

            world.spawnParticle(Particle.SQUID_INK, point, 1, 0.05, 0.1, 0.05, 0.0);
            if (i % 3 == 0) {
                world.spawnParticle(Particle.SMOKE, point, 1, 0.05, 0.2, 0.05, 0.01);
            }
        }

        world.spawnParticle(Particle.SOUL, center, 4, 0.35, 0.5, 0.35, 0.01);
    }

    public boolean hasShield(Player player) {
        return getDamageReduction(player) > 0.0;
    }
}