package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.ArrayList;
import java.util.List;

public class LastStand implements Ability {

    private static final String SHIELD_TITLE = "LAST STAND";
    private static final Color SPECTRAL_CYAN = Color.fromRGB(80, 255, 220);
    private static final Color SPECTRAL_BLUE = Color.fromRGB(20, 90, 140);
    private static final Color SPECTRAL_DARK = Color.fromRGB(10, 40, 60);

    private static final Particle.DustTransition AURA_DUST =
            new Particle.DustTransition(SPECTRAL_CYAN, SPECTRAL_BLUE, 1.2f);
    private static final Particle.DustTransition RING_DUST =
            new Particle.DustTransition(SPECTRAL_CYAN, SPECTRAL_DARK, 1.4f);
    private static final Particle.DustTransition BEAM_DUST =
            new Particle.DustTransition(Color.fromRGB(120, 255, 230), Color.fromRGB(30, 140, 120), 0.9f);

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double damageReduction;
    private final double healPerSecondPerEnemy;
    private final double radius;
    private final long tickIntervalMs;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;

    public LastStand(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                     PlayerHealthManager healthManager, ShieldManager shieldManager,
                     CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 24000L);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.damageReduction = config.getDouble("damage-reduction", 0.30);
        this.healPerSecondPerEnemy = config.getDouble("heal-per-second-per-enemy", 25.0);
        this.radius = config.getDouble("radius", 8.0);
        this.tickIntervalMs = Math.max(100L, config.getLong("tick-interval-ms", 1000L));

        this.cost = new CooldownCost(cooldownManager, "laststand", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
    }

    @Override
    public String getName() {
        return "Last Stand";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        shieldManager.applyShield(player, damageReduction, durationMs, SHIELD_TITLE);

        playActivationEffects(player);

        final long visualPeriodTicks = 2L;
        final long visualPeriodMs = visualPeriodTicks * 50L;

        new BukkitRunnable() {
            long elapsedMs = 0;
            long healAccumulatorMs = 0;
            int frame = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR || elapsedMs >= durationMs) {
                    playEndEffects(player);
                    cancel();
                    return;
                }

                Location base = player.getLocation();
                List<Player> enemies = getEnemiesInRange(player);

                drawAura(base, frame);
                drawOuterRing(base, frame);
                drawRisingSouls(base, frame);

                for (Player enemy : enemies) {
                    drawDrainBeam(enemy.getLocation().add(0, 1.0, 0), base.clone().add(0, 1.0, 0), frame);
                }

                if (frame % 4 == 0) {
                    base.getWorld().playSound(base, Sound.PARTICLE_SOUL_ESCAPE, 0.6f, 0.8f + (frame % 8) * 0.03f);
                }

                healAccumulatorMs += visualPeriodMs;
                if (healAccumulatorMs >= tickIntervalMs) {
                    healAccumulatorMs -= tickIntervalMs;

                    if (!enemies.isEmpty()) {
                        double healAmount = healPerSecondPerEnemy * enemies.size() * (tickIntervalMs / 1000.0);
                        healthManager.heal(player, healAmount, getName());
                        playHealPulse(player, enemies.size());
                    }
                }

                elapsedMs += visualPeriodMs;
                frame++;
            }
        }.runTaskTimer(plugin, 0L, visualPeriodTicks);

        return true;
    }



    private void playActivationEffects(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        Location center = loc.clone().add(0, 1.0, 0);

        world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_DEATH, 1.2f, 0.6f);
        world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_AMBIENT, 1.0f, 0.5f);
        world.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 1.5f, 0.5f);
        world.playSound(loc, Sound.ENTITY_VEX_CHARGE, 0.8f, 0.6f);

        world.spawnParticle(Particle.SCULK_SOUL, center, 40, 0.6, 0.8, 0.6, 0.08);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 50, 0.5, 0.7, 0.5, 0.12);
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, center, 40, 0.7, 0.9, 0.7, AURA_DUST);

        new BukkitRunnable() {
            int step = 1;
            final int steps = 8;

            @Override
            public void run() {
                if (step > steps) {
                    cancel();
                    return;
                }
                double r = radius * ((double) step / steps);
                Location ground = loc.clone().add(0, 0.1, 0);
                int points = (int) Math.max(24, r * 10);

                for (int i = 0; i < points; i++) {
                    double angle = (Math.PI * 2.0 / points) * i;
                    Location p = ground.clone().add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.02, 0.05, 0.02, 0.02);
                    world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p.clone().add(0, 0.3, 0), 1, 0, 0, 0, RING_DUST);
                    if (i % 3 == 0) {
                        world.spawnParticle(Particle.SOUL, p.clone().add(0, 0.2, 0), 1, 0, 0.1, 0, 0.03);
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawAura(Location base, int frame) {
        World world = base.getWorld();
        double angleOffset = frame * 0.5;

        for (int arm = 0; arm < 3; arm++) {
            double angle = angleOffset + arm * (Math.PI * 2.0 / 3.0);
            for (double y = 0.1; y <= 2.0; y += 0.45) {
                double r = 0.75 + Math.sin(frame * 0.3 + y * 2.0) * 0.12;
                double a = angle + y * 1.2;
                Location p = base.clone().add(Math.cos(a) * r, y, Math.sin(a) * r);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 1, 0.02, 0.02, 0.02, 0.0);
                world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.05, 0.05, 0.05, AURA_DUST);
            }
        }
    }

    private void drawOuterRing(Location base, int frame) {
        World world = base.getWorld();
        Location ground = base.clone().add(0, 0.1, 0);
        int points = 40;
        double rotation = frame * 0.12;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 / points) * i + rotation;
            Location p = ground.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0, 0, 0, RING_DUST);

            if (i % 4 == (frame % 4)) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, p.clone().add(0, 0.15, 0), 1, 0.03, 0.12, 0.03, 0.01);
            }
        }

        double innerR = radius * 0.55;
        int innerPoints = 20;
        for (int i = 0; i < innerPoints; i++) {
            double angle = (Math.PI * 2.0 / innerPoints) * i - rotation * 1.5;
            Location p = ground.clone().add(Math.cos(angle) * innerR, 0, Math.sin(angle) * innerR);
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0, 0, 0, AURA_DUST);
        }
    }

    private void drawRisingSouls(Location base, int frame) {
        World world = base.getWorld();
        if (frame % 2 != 0) return;

        for (int i = 0; i < 4; i++) {
            double angle = Math.random() * Math.PI * 2.0;
            double dist = Math.random() * radius;
            Location p = base.clone().add(Math.cos(angle) * dist, 0.1, Math.sin(angle) * dist);
            world.spawnParticle(Particle.SOUL, p, 1, 0.05, 0.1, 0.05, 0.04);
        }
        world.spawnParticle(Particle.SCULK_SOUL, base.clone().add(0, 0.8, 0), 1, 0.5, 0.6, 0.5, 0.01);
    }

    private void drawDrainBeam(Location from, Location to, int frame) {
        World world = from.getWorld();
        Vector dir = to.toVector().subtract(from.toVector());
        double length = dir.length();
        if (length < 0.5) return;
        dir.normalize();

        double step = 0.6;
        double pulse = (frame * 0.7) % length;

        for (double d = 0; d < length; d += step) {
            Location p = from.clone().add(dir.clone().multiply(d));
            double wobble = Math.sin(d * 2.0 + frame * 0.6) * 0.15;
            p.add(0, wobble, 0);

            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, p, 1, 0.03, 0.03, 0.03, BEAM_DUST);

            if (Math.abs(d - pulse) < step) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, p, 2, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.SOUL, p, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }

        world.spawnParticle(Particle.SCULK_SOUL, from, 2, 0.3, 0.5, 0.3, 0.02);
    }

    private void playHealPulse(Player player, int enemyCount) {
        Location loc = player.getLocation().add(0, 1.0, 0);
        World world = loc.getWorld();

        world.spawnParticle(Particle.HEART, loc.clone().add(0, 0.6, 0), Math.min(enemyCount, 4), 0.4, 0.3, 0.4, 0.0);
        world.spawnParticle(Particle.SCULK_SOUL, loc, 10 * enemyCount, 0.5, 0.7, 0.5, 0.06);
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 12, 0.5, 0.7, 0.5, AURA_DUST);

        world.playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 0.9f, 1.0f);
        world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, 0.7f, 0.6f);
    }

    private void playEndEffects(Player player) {
        if (!player.isOnline()) return;
        Location loc = player.getLocation().add(0, 1.0, 0);
        World world = loc.getWorld();

        world.playSound(loc, Sound.ENTITY_VEX_DEATH, 1.0f, 0.6f);
        world.playSound(loc, Sound.ENTITY_SKELETON_HORSE_AMBIENT, 0.8f, 0.4f);

        world.spawnParticle(Particle.SCULK_SOUL, loc, 30, 0.6, 0.8, 0.6, 0.1);
        world.spawnParticle(Particle.SOUL, loc, 25, 0.6, 0.8, 0.6, 0.08);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.5, 0.7, 0.5, 0.05);
    }

    private List<Player> getEnemiesInRange(Player player) {
        List<Player> result = new ArrayList<>();
        double radiusSquared = radius * radius;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player candidate)) continue;
            if (candidate.equals(player) || candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, candidate)) continue;
            if (candidate.getLocation().distanceSquared(player.getLocation()) > radiusSquared) continue;
            result.add(candidate);
        }

        return result;
    }

    @Override
    public String getDescription() {
        return "For " + (durationMs / 1000L) + "s you take " + Math.round(damageReduction * 100)
                + "% less damage and heal " + (int) healPerSecondPerEnemy
                + " HP per second for each enemy within " + (int) radius + " blocks.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Reduction", Math.round(damageReduction * 100) + "%"),
                new AbilityStat("Heal per Enemy", (int) healPerSecondPerEnemy + " HP/s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}