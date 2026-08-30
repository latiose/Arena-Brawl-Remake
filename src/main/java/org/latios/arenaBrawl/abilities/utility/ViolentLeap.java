package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViolentLeap implements Ability {
    ;
    private static final double RADIUS = 3.0;
    private static final int COOLDOWN_SECONDS = 40;

    private final AbilityCost cost;
    private final TeamManager teamManager;


    public ViolentLeap(CooldownManager cooldownManager, TeamManager teamManager,
                       CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "hazard_leap", COOLDOWN_SECONDS * 1000, combatUpgradeManager);
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Violent Leap"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Leaps forward into the air and slams forcefully onto the ground knocking back nearby enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Radius", RADIUS + " blocks"),
                new AbilityStat("Cooldown", COOLDOWN_SECONDS + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        Vector leapVelocity = direction.multiply(1.2).setY(0.9);
        player.setVelocity(leapVelocity);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RABBIT_JUMP, 1.0f, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                loc.getWorld().spawnParticle(
                        Particle.DUST,
                        loc,
                        5,
                        0.2, 0.2, 0.2,
                        new Particle.DustOptions(Color.fromRGB(200, 40, 20), 1.3f)
                );
                loc.getWorld().spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0.05);

                if (ticks > 5 && player.isOnGround()) {
                    cancel();

                    Location impactLoc = player.getLocation();

                    impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.4f);
                    impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_ANVIL_FALL, 0.8f, 0.6f);

                    impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc.clone().add(0, 0.5, 0), 1, 0, 0, 0, 0);
                    impactLoc.getWorld().spawnParticle(
                            Particle.DUST,
                            impactLoc.clone().add(0, 0.2, 0),
                            40,
                            1.2, 0.2, 1.2,
                            new Particle.DustOptions(Color.fromRGB(120, 30, 10), 1.8f)
                    );

                    drawSpikeRing(impactLoc, RADIUS);

                    Set<Player> targets = new HashSet<>();
                    for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, RADIUS, 2.5, RADIUS)) {
                        if (entity instanceof Player victim && !victim.equals(player)) {
                            if (teamManager.isEnemy(player, victim)) {
                                targets.add(victim);
                            }
                        }
                    }

                    for (Player victim : targets) {

                        Vector kb = victim.getLocation().toVector().subtract(impactLoc.toVector()).setY(0);
                        if (kb.lengthSquared() > 0) {
                            kb.normalize().multiply(1.1).setY(0.4);
                        } else {
                            kb = new Vector(0, 0.4, 0);
                        }
                        victim.setVelocity(kb);
                    }
                }

                if (ticks > 60) {
                    cancel();
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 2L, 1L);

        return true;
    }

    private void drawSpikeRing(Location center, double radius) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location pLoc = center.clone().add(x, 0.1, z);
            pLoc.getWorld().spawnParticle(Particle.BLOCK, pLoc, 3, 0.1, 0.2, 0.1, pLoc.getBlock().getRelative(0, -1, 0).getBlockData());
        }
    }
}