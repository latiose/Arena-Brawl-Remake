package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.abilities.structures.ProjectileInterceptionService;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class JudgmentLance implements Ability {

    private final double maxRange;
    private final double beamRadius;
    private final double damage;
    private final int telegraphTicks;
    private final long chargeTimeMillis;
    private final double burnRadius;
    private final double burnDamage;
    private final int burnDurationTicks;
    private final int burnPulseTicks;

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityCost cost;

    public JudgmentLance(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                         TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.combatService = combatService;

        this.maxRange = config.getDouble("max-range", 256.0);
        this.beamRadius = config.getDouble("beam-radius", 1.5);
        this.damage = config.getDouble("damage", 450.0);
        this.telegraphTicks = config.getInt("telegraph-ticks", 30);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.burnRadius = config.getDouble("burn-radius", 2.5);
        this.burnDamage = config.getDouble("burn-damage", 50.0);
        this.burnDurationTicks = config.getInt("burn-duration-ticks", 60);
        this.burnPulseTicks = Math.max(1, config.getInt("burn-pulse-ticks", 10));

        this.cost = new UltimateCost(cooldownManager, usageManager, "judgmentlance");
    }

    @Override
    public String getName() {
        return "Judgment Lance";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "judgmentlance", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Channels a map-wide piercing beam after a short telegraph, dealing massive damage in a line and leaving a burning zone.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Telegraph", String.format("%.1fs", telegraphTicks / 20.0)),
                new AbilityStat("Burn Duration", (burnDurationTicks / 20) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getEyeLocation();
        Vector direction = origin.getDirection().normalize();

        double length = 0;

        for (double traveled = 0.5; traveled <= maxRange; traveled += 0.5) {
            Location point = origin.clone().add(direction.clone().multiply(traveled));
            if (point.getBlock().getType().isSolid() || ProjectileInterceptionService.isBlocked(player, point)) {
                break;
            }
            length = traveled;
        }

        final double beamLength = length;

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 10.0f, 1.4f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                drawTelegraph(origin, direction, beamLength);

                if (ticks % 8 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 6.0f, 1.8f);
                }

                if (ticks >= telegraphTicks - 1) {
                    fire(player, origin, direction, beamLength);
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void fire(Player caster, Location origin, Vector direction, double beamLength) {
        origin.getWorld().playSound(origin, Sound.ENTITY_WARDEN_SONIC_BOOM, 10.0f, 1.2f);
        origin.getWorld().playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 10.0f, 1.6f);

        drawBeam(origin, direction, beamLength);

        java.util.Set<Player> hit = new java.util.HashSet<>();

        for (Player enemy : origin.getWorld().getPlayers()) {
            if (enemy.equals(caster) || enemy.isDead() || enemy.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, enemy)) continue;
            if (!hit.add(enemy)) continue;

            Vector toEnemy = enemy.getLocation().add(0, 1.0, 0).toVector().subtract(origin.toVector());
            double projection = toEnemy.dot(direction);
            if (projection < 0 || projection > beamLength) continue;

            Vector closest = origin.toVector().add(direction.clone().multiply(projection));
            if (toEnemy.lengthSquared() < 1e-6) continue;
            double perpendicular = enemy.getLocation().add(0, 1.0, 0).toVector().distance(closest);
            if (perpendicular > beamRadius) continue;

            combatService.applyAbilityDamage(caster, enemy, damage, getName(), enemy.getLocation().add(0, 1.0, 0));
          //  enemy.getWorld().spawnParticle(Particle.FLASH, enemy.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, 0);
            enemy.getWorld().spawnParticle(Particle.CRIT, enemy.getLocation().add(0, 1.0, 0), 25, 0.3, 0.5, 0.3, 0.1);
        }

        startBurningZone(caster, origin, direction, beamLength);
    }

    private void startBurningZone(Player caster, Location origin, Vector direction, double beamLength) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= burnDurationTicks) {
                    cancel();
                    return;
                }

                drawBurningZone(origin, direction, beamLength);

                if (ticks > 0 && ticks % burnPulseTicks == 0) {
                    origin.getWorld().playSound(origin, Sound.BLOCK_FIRE_AMBIENT, 4.0f, 1.2f);

                    for (Player enemy : origin.getWorld().getPlayers()) {
                        if (enemy.equals(caster) || enemy.isDead() || enemy.getGameMode() == GameMode.SPECTATOR) continue;
                        if (!teamManager.isEnemy(caster, enemy)) continue;

                        Vector toEnemy = enemy.getLocation().add(0, 1.0, 0).toVector().subtract(origin.toVector());
                        double projection = toEnemy.dot(direction);
                        if (projection < 0 || projection > beamLength) continue;

                        Vector closest = origin.toVector().add(direction.clone().multiply(projection));
                        if (toEnemy.lengthSquared() < 1e-6) continue;
                        double distance = enemy.getLocation().add(0, 1.0, 0).toVector().distance(closest);
                        if (distance > burnRadius) continue;

                        combatService.applyAbilityDamage(caster, enemy, burnDamage, getName());
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void drawTelegraph(Location origin, Vector direction, double beamLength) {
        for (double traveled = 0; traveled <= beamLength; traveled += 2.0) {
            Location point = origin.clone().add(direction.clone().multiply(traveled));
            point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.15, 0.15, 0.15, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 200, 60), 1.2f));
        }

        Location tip = origin.clone().add(direction.clone().multiply(beamLength));
        tip.getWorld().spawnParticle(Particle.END_ROD, tip, 4, 0.2, 0.2, 0.2, 0.02);
    }

    private void drawBeam(Location origin, Vector direction, double beamLength) {
        for (double traveled = 0; traveled <= beamLength; traveled += 0.35) {
            Location point = origin.clone().add(direction.clone().multiply(traveled));
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0, 0, 0, 0);
        }

        for (double traveled = 0; traveled <= beamLength; traveled += 1.2) {
            Location point = origin.clone().add(direction.clone().multiply(traveled));
            point.getWorld().spawnParticle(Particle.FIREWORK, point, 2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    private void drawBurningZone(Location origin, Vector direction, double beamLength) {
        for (double traveled = 0; traveled <= beamLength; traveled += 2.0) {
            Location point = origin.clone().add(direction.clone().multiply(traveled));

            point.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.3, 0.1, 0.3, 0.005);
            if (Math.random() < 0.35) {
                point.getWorld().spawnParticle(Particle.LAVA, point, 1, 0.4, 0.1, 0.4, 0);
            }
        }
    }
}
