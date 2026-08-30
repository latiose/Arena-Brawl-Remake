package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MagneticImpulse implements Ability {

    private static final double RADIUS = 5.0;
    private static final long STUN_DURATION_MILLIS = 2_000;
    private static final int COOLDOWN_SECONDS = 40;

    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public MagneticImpulse(CooldownManager cooldownManager, DebuffManager debuffManager,
                           CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "magnetic_impulse", COOLDOWN_SECONDS * 1000, combatUpgradeManager);
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Magnetic Impulse"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Emits a strong magnetic pulse that pulls all nearby players towards you and immobilizes them for 2 seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immobilize", "2s"),
                new AbilityStat("Radius", RADIUS + " blocks"),
                new AbilityStat("Cooldown", COOLDOWN_SECONDS + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location casterLoc = player.getLocation();

        casterLoc.getWorld().playSound(casterLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.5f);
        casterLoc.getWorld().playSound(casterLoc, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.8f);

        Set<Player> targets = new HashSet<>();
        for (Entity entity : casterLoc.getWorld().getNearbyEntities(casterLoc, RADIUS, RADIUS, RADIUS)) {
            if (entity instanceof Player victim && !victim.equals(player)) {
                targets.add(victim);
            }
        }
        Location location = player.getLocation();
        for (Player victim : targets) {
            victim.teleport(location);
            victim.getWorld().playSound(victim.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);

            Bukkit.getScheduler().runTaskLater(ArenaBrawlPlugin.getInstance(), () -> {
                if (victim.isOnline() && !victim.isDead()) {
                    debuffManager.tryApply(victim, DebuffType.IMMOBILIZE, STUN_DURATION_MILLIS);
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

                double currentRadius = RADIUS * (1.0 - (ticks / 10.0));
                drawImplosionRing(player.getLocation().add(0, 1, 0), currentRadius);
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void drawImplosionRing(Location center, double radius) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

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