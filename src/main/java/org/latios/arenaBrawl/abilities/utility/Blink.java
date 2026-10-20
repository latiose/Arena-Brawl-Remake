package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Blink implements Ability {

    private static final double STEP_SIZE = 0.25;

    private final double maxDistance;
    private final long cooldownMs;
    private final AbilityCost cost;
    private final AbilityConfig config;

    public Blink(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                 AbilityConfig config) {
        this.maxDistance = config.getDouble("max-distance", 10.0);
        this.cooldownMs = config.getLong("cooldown-ms", 18000L);
        this.cost = new CooldownCost(cooldownManager, "blink", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Blink";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Instantly teleports a short distance in the direction you are looking. Stops at walls.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Distance", (int) maxDistance + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection();
        if (direction.lengthSquared() < 0.0001) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }
        direction.normalize();

        Location lastSafe = origin.clone();
        double traveled = 0;
        while (traveled < maxDistance) {
            Location next = lastSafe.clone().add(direction.clone().multiply(STEP_SIZE));
            if (isBlocked(next) || !isInWorld(next)) {
                break;
            }
            lastSafe = next;
            traveled += STEP_SIZE;
        }

        if (origin.distanceSquared(lastSafe) < 1.0) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        lastSafe.setYaw(origin.getYaw());
        lastSafe.setPitch(origin.getPitch());

        origin.getWorld().spawnParticle(Particle.PORTAL, origin.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.2);
        player.teleport(lastSafe);
        player.setFallDistance(0);
        lastSafe.getWorld().spawnParticle(Particle.PORTAL, lastSafe.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.2);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10.0f, 1.6f);

        return true;
    }

    private boolean isBlocked(Location destination) {
        return destination.getBlock().getType().isSolid()
                || destination.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private boolean isInWorld(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        int y = location.getBlockY();
        return y >= location.getWorld().getMinHeight() && y < location.getWorld().getMaxHeight() - 1;
    }
}
