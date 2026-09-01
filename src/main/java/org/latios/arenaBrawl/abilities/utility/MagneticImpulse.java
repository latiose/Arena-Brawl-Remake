package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagneticImpulse implements Ability {

    private final double radius;
    private final long stunDurationMillis;
    private final long cooldownMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public MagneticImpulse(Plugin plugin, CooldownManager cooldownManager, DebuffManager debuffManager,
                           CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.radius = config.getDouble("radius", 5.0);
        this.stunDurationMillis = config.getLong("stun-duration-ms", 2000L);
        this.cooldownMs = config.getLong("cooldown-ms", 40000L);

        this.cost = new CooldownCost(cooldownManager, "magnetic_impulse", cooldownMs, combatUpgradeManager);
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Magnetic Impulse"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Emits a strong magnetic pulse that pulls all nearby players towards you and immobilizes them for " + (stunDurationMillis / 1000L) + " seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immobilize", (stunDurationMillis / 1000L) + "s"),
                new AbilityStat("Radius", radius + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location casterLoc = player.getLocation();

        casterLoc.getWorld().playSound(casterLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.5f);
        casterLoc.getWorld().playSound(casterLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.8f);

        Set<Player> targets = new HashSet<>();
        for (Entity entity : casterLoc.getWorld().getNearbyEntities(casterLoc, radius, radius, radius)) {
            if (entity instanceof Player victim && !victim.equals(player)) {
                targets.add(victim);
            }
        }
        Location location = player.getLocation();
        for (Player victim : targets) {
            victim.teleport(location);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (victim.isOnline() && !victim.isDead()) {
                    debuffManager.tryApply(victim, DebuffType.IMMOBILIZE, stunDurationMillis);
                }
            }, 2L);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (ticks > 10 || !player.isOnline()) {
                    cancel();
                    return;
                }

                double currentRadius = radius * (1.0 - (ticks / 10.0));
                drawImplosionRing(player.getLocation().add(0, 1, 0), currentRadius);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void drawImplosionRing(Location center, double currentRadius) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = currentRadius * Math.cos(angle);
            double z = currentRadius * Math.sin(angle);

            Location pLoc = center.clone().add(x, 0, z);
            pLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    pLoc,
                    1,
                    new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.3f)
            );
            pLoc.getWorld().spawnParticle(Particle.CRIT, pLoc, 1, 0, 0, 0, 0);
        }
    }
}