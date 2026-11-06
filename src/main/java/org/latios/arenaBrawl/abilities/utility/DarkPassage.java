package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class DarkPassage implements Ability {

    private final double maxRange;
    private final double backDashDistance;
    private final long durationTicks;
    private final long cooldownMs;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final Plugin plugin;

    public DarkPassage(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.maxRange = config.getDouble("max-range", 12.0);
        this.backDashDistance = config.getDouble("back-dash", 7.0);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);

        this.cost = new CooldownCost(cooldownManager, "darkpassage", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Dark Passage"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) maxRange);
        if (targetBlock == null) {
            player.sendMessage("§eTarget location too far or invalid!");
            return false;
        }

        Location lanternLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 10.0f, 0.8f);


        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= durationTicks || !player.isOnline() || player.isDead()) {
                    lanternLoc.getWorld().spawnParticle(Particle.SMOKE, lanternLoc, 15, 0.2, 0.2, 0.2, 0.05);
                    cancel();
                    return;
                }

                lanternLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, lanternLoc, 3, 0.1, 0.1, 0.1, 0.01);
                lanternLoc.getWorld().spawnParticle(Particle.SOUL, lanternLoc, 1, 0.1, 0.1, 0.1, 0.02);

                for (Player occupant : lanternLoc.getWorld().getPlayers()) {
                    boolean isOwner = occupant.equals(player);
                    if (!isOwner && !teamManager.isAlly(player, occupant)) continue;

                    if (occupant.getLocation().distanceSquared(lanternLoc) > 2.25) continue;

                    if (isOwner) {
                        dashBackwards(occupant);
                    } else {
                        Location destination = player.getLocation();
                        occupant.teleport(destination);

                        occupant.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        occupant.getWorld().spawnParticle(Particle.PORTAL, destination.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
                    }

                    cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    /**
     * The owner touching their own lantern dashes backwards (opposite to the
     * view direction) up to the configured distance without clipping into
     * walls: the landing spot is the farthest free position along the
     * backwards ray.
     */
    private void dashBackwards(Player owner) {
        Location eye = owner.getEyeLocation();
        Vector backward = eye.getDirection().normalize().multiply(-1.0);

        // How far the backwards ray can actually travel: a wall stops it and
        // the dash never clips through blocks.
        double freeDistance = backDashDistance;
        RayTraceResult hit = owner.getWorld().rayTraceBlocks(eye, backward, backDashDistance);
        if (hit != null) {
            freeDistance = Math.max(0.0, hit.getHitPosition().distance(eye.toVector()) - 0.5);
        }

        Location destination = null;
        for (double d = freeDistance; d >= 0.0; d -= 0.25) {
            Location candidate = owner.getLocation().add(backward.clone().multiply(d));
            if (isSafe(candidate)) {
                destination = candidate;
                break;
            }
        }

        // Nowhere safe along the backwards ray: stay in place.
        if (destination == null) return;

        destination.setYaw(owner.getLocation().getYaw());
        destination.setPitch(owner.getLocation().getPitch());

        owner.setFallDistance(0);
        owner.teleport(destination);

        owner.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.9f);
        owner.getWorld().playSound(destination, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 1.4f);
        owner.getWorld().spawnParticle(Particle.PORTAL, destination.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        owner.getWorld().spawnParticle(Particle.SOUL, eye.clone().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.05);
    }

    private boolean isSafe(Location location) {
        if (location.getWorld() == null) return false;
        if (location.getBlock().getType().isSolid()) return false;
        if (location.clone().add(0, 1, 0).getBlock().getType().isSolid()) return false;

        int y = location.getBlockY();
        return y >= location.getWorld().getMinHeight() && y < location.getWorld().getMaxHeight() - 1;
    }

    @Override
    public String getDescription() {
        return "Places a soul lantern. If an ally steps on it, they instantly teleport to your position. "
                + "Touch it yourself to dash backwards without passing through walls.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationTicks / 20L) + "s"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Back Dash", (int) backDashDistance + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}