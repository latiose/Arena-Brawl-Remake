package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class GlacialPath implements Ability {

    private final Plugin plugin;
    private final AbilityConfig config;
    private final long cooldownMs;
    private final long durationMs;
    private final double speed;
    private final double maxDistance;
    private final double immobilizeRadius;
    private final long immobilizeDurationMs;

    private final CooldownCost delegate;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;

    private BukkitRunnable task;
    private Location claw;
    private Vector direction;
    private boolean active;
    private long startedAt;
    private double traveled;
    private AbilitySlot lastSlot;

    public GlacialPath(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager,
                       AbilityConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.speed = config.getDouble("speed", 0.4);
        this.maxDistance = config.getDouble("max-distance", 50.0);
        this.immobilizeRadius = config.getDouble("immobilize-radius", 4.0);
        this.immobilizeDurationMs = config.getLong("immobilize-duration-ms", 1500L);

        this.delegate = new CooldownCost(cooldownManager, "glacialpath", cooldownMs, combatUpgradeManager);
        this.cost = new GlacialPathCost();
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
    }

    private final class GlacialPathCost implements AbilityCost {

        @Override
        public boolean canPay(Player player) {
            return canPay(player, null);
        }

        @Override
        public boolean canPay(Player player, AbilitySlot slot) {
            return isActive() || delegate.canPay(player, slot);
        }

        @Override
        public void pay(Player player) {
            pay(player, null);
        }

        @Override
        public void pay(Player player, AbilitySlot slot) {
            lastSlot = slot;
        }

        @Override
        public String describeRemaining(Player player) {
            return delegate.describeRemaining(player);
        }

        @Override
        public int getRemainingSeconds(Player player) {
            return delegate.getRemainingSeconds(player);
        }

        @Override
        public String getBaseCostDescription() {
            return delegate.getBaseCostDescription();
        }
    }

    @Override
    public String getName() {
        return "Glacial Path";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    private boolean isActive() {
        return active;
    }

    @Override
    public boolean activate(Player player) {
        if (isActive()) {
            return teleportToPath(player);
        }

        startPath(player);
        return true;
    }

    private void startPath(Player player) {
        direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();

        final Vector step = direction.clone().multiply(speed);

        claw = player.getLocation().clone();
        traveled = 0;
        active = true;
        startedAt = System.currentTimeMillis();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.6f);
        drawClaw(claw);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active) {
                    cancel();
                    return;
                }

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !config.getMatchManager().isInMatch(player)
                        || System.currentTimeMillis() - startedAt >= durationMs) {
                    endPath(player);
                    return;
                }

                claw.add(step);
                traveled += speed;
                drawClaw(claw);

                if (traveled >= maxDistance) {
                    endPath(player);
                }
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
    }

    private boolean teleportToPath(Player player) {
        if (!active || claw == null) {
            return false;
        }

        Location destination = safeDestination(claw);
        destination.setDirection(player.getLocation().getDirection());

        player.teleport(destination);
        player.setFallDistance(0);

        destination.getWorld().spawnParticle(Particle.SNOWFLAKE, destination.clone().add(0, 1.0, 0),
                45, 0.6, 0.8, 0.6, 0.05);
        destination.getWorld().spawnParticle(Particle.END_ROD, destination.clone().add(0, 1.0, 0),
                25, 0.5, 0.7, 0.5, 0.04);
        destination.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, destination.clone().add(0, 1.0, 0),
                20, 0.5, 0.6, 0.5, 0.08);
        destination.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
        destination.getWorld().playSound(destination, Sound.BLOCK_GLASS_BREAK, 0.9f, 1.6f);

        immobilizeEnemies(player, destination);
        endPath(player);
        return true;
    }

    private void immobilizeEnemies(Player caster, Location center) {
        double radiusSquared = immobilizeRadius * immobilizeRadius;

        for (Entity entity : center.getWorld().getNearbyEntities(center, immobilizeRadius, immobilizeRadius, immobilizeRadius)) {
            if (!(entity instanceof Player target)) continue;
            if (target.equals(caster) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(center) > radiusSquared) continue;

            debuffManager.tryApply(caster, target, DebuffType.IMMOBILIZE, immobilizeDurationMs);

            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1.0, 0),
                    20, 0.35, 0.6, 0.35, 0.03);
            target.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, target.getLocation().add(0, 0.5, 0),
                    15, 0.35, 0.4, 0.35, 0.05);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1.4f);
        }
    }

    private void endPath(Player player) {
        if (!active) return;

        active = false;
        claw = null;

        if (task != null) {
            task.cancel();
            task = null;
        }

        if (player.isOnline()) {
            if (lastSlot != null) {
                delegate.pay(player, lastSlot);
            } else {
                delegate.pay(player);
            }
        }
    }

    private Location safeDestination(Location base) {
        Location destination = base.clone();

        for (int i = 0; i < 4 && isBlocked(destination); i++) {
            destination.add(0, 1, 0);
        }

        return destination;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private void drawClaw(Location location) {
        Location center = location.clone().add(0, 1.0, 0);
        Vector forward = direction.clone().multiply(0.6);
        Vector side = new Vector(-direction.getZ(), 0, direction.getX());

        for (int i = -1; i <= 1; i++) {
            Location prong = center.clone().add(side.clone().multiply(i * 0.35));
            prong.getWorld().spawnParticle(Particle.SNOWFLAKE, prong, 2, 0.04, 0.12, 0.04, 0.0);

            Location tip = prong.clone().add(forward);
            tip.getWorld().spawnParticle(Particle.END_ROD, tip, 1, 0.03, 0.08, 0.03, 0.0);
            tip.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, tip, 1, 0.05, 0.1, 0.05, 0.02);
        }
    }

    @Override
    public String getDescription() {
        return "Launches a claw of ice that advances slowly for " + (durationMs / 1000L)
                + "s. Reactivate the ability to teleport to the claw and immobilize enemies within "
                + (int) immobilizeRadius + " blocks for " + (immobilizeDurationMs / 1000L)
                + "s. The cooldown starts when the path ends.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Path Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Immobilize", (immobilizeDurationMs / 1000L) + "s"),
                new AbilityStat("Immobilize Radius", (int) immobilizeRadius + " blocks"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
