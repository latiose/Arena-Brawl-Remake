package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Shared state for Soul Link. Damage taken by one linked enemy is partially
 * mirrored to the other linked enemy.
 */
public class SoulLinkManager {

    private static final class Link {
        private UUID first;
        private UUID second;
        private long expiresAt;
        private BukkitTask task;
    }

    private final Map<UUID, Link> links = new HashMap<>();
    private final Plugin plugin;
    private final PlayerHealthManager healthManager;
    private final double reflectPercent;
    private boolean resolvingMirror = false;

    public SoulLinkManager(Plugin plugin, PlayerHealthManager healthManager, AbilityConfig config) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.reflectPercent = config.getDouble("reflect-percent", 0.30);
    }

    public void link(Player first, Player second, long durationMs) {
        if (first == null || second == null || first.equals(second)) return;

        clear(first);
        clear(second);

        Link link = new Link();
        link.first = first.getUniqueId();
        link.second = second.getUniqueId();
        link.expiresAt = System.currentTimeMillis() + durationMs;

        links.put(link.first, link);
        links.put(link.second, link);

        link.task = new BukkitRunnable() {
            @Override
            public void run() {
                Player a = Bukkit.getPlayer(link.first);
                Player b = Bukkit.getPlayer(link.second);
                if (System.currentTimeMillis() >= link.expiresAt
                        || !isValid(a) || !isValid(b)) {
                    clearLink(link);
                    cancel();
                    return;
                }

                drawLink(a.getEyeLocation(), b.getEyeLocation());
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public Player getPartner(Player victim) {
        Link link = links.get(victim.getUniqueId());
        if (link == null) return null;

        if (System.currentTimeMillis() >= link.expiresAt) {
            clearLink(link);
            return null;
        }

        UUID partnerId = link.first.equals(victim.getUniqueId()) ? link.second : link.first;
        Player partner = Bukkit.getPlayer(partnerId);
        if (!isValid(partner)) {
            clearLink(link);
            return null;
        }
        return partner;
    }

    public void applyMirrorDamage(Player victim, Player attacker, double finalDamage) {
        if (resolvingMirror || finalDamage <= 0) return;

        Player partner = getPartner(victim);
        if (partner == null || partner.equals(victim)) return;

        double mirroredDamage = finalDamage * reflectPercent;
        if (mirroredDamage <= 0) return;

        resolvingMirror = true;
        try {
            healthManager.damage(partner, mirroredDamage, attacker);
        } finally {
            resolvingMirror = false;
        }

        partner.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR, partner.getLocation().add(0, 1.0, 0), 8,
                0.3, 0.4, 0.3, 0.05
        );
        partner.getWorld().spawnParticle(
                Particle.SOUL, partner.getLocation().add(0, 1.0, 0), 6,
                0.25, 0.35, 0.25, 0.01
        );
        partner.getWorld().playSound(
                partner.getLocation(), Sound.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS, 0.8f, 1.3f
        );
    }

    public void clear(Player player) {
        if (player == null) return;
        Link link = links.get(player.getUniqueId());
        if (link != null) {
            clearLink(link);
        }
    }

    public void clearAll() {
        for (Link link : new HashSet<>(links.values())) {
            clearLink(link);
        }
    }

    private void clearLink(Link link) {
        if (link == null) return;
        links.remove(link.first);
        links.remove(link.second);
        if (link.task != null) {
            link.task.cancel();
            link.task = null;
        }
    }

    private boolean isValid(Player player) {
        return player != null
                && player.isOnline()
                && !player.isDead()
                && player.getGameMode() != GameMode.SPECTATOR;
    }

    private void drawLink(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.45);
        Location current = start.clone();
        double traveled = 0;
        while (traveled < length) {
            current.getWorld().spawnParticle(Particle.SOUL, current, 1, 0, 0, 0, 0.005);
            current.add(direction);
            traveled += 0.45;
        }
    }
}
