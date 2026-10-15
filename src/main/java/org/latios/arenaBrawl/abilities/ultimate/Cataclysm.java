package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Cataclysm implements Ability {

    private final double maxRange;
    private final double impactRadius;
    private final double damage;
    private final double knockbackStrength;
    private final long warningTicks;
    private final long chargeTimeMillis;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final StructureDemolitionService demolitionService;
    private final AbilityConfig config;

    public Cataclysm(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                     TeamManager teamManager, CombatService combatService,
                     StructureDemolitionService demolitionService, AbilityConfig config) {
        this.plugin = plugin;
        this.maxRange = config.getDouble("max-range", 24.0);
        this.impactRadius = config.getDouble("impact-radius", 6.0);
        this.damage = config.getDouble("damage", 380.0);
        this.knockbackStrength = config.getDouble("knockback-strength", 1.6);
        this.warningTicks = config.getLong("warning-ticks", 16L);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "cataclysm");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.demolitionService = demolitionService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Cataclysm";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "cataclysm", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Marks the ground you are aiming at. After a short delay, a massive smash deals heavy damage "
                + "and demolishes enemy structures in the area.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Radius", (int) impactRadius + "m"),
                new AbilityStat("Range", (int) maxRange + "m"),
                new AbilityStat("Breaks structures", "Yes"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location impact = resolveTargetLocation(player);
        MatchSoundUtils.play(config, player, Sound.ENTITY_WITHER_SHOOT, 0.8f, 0.5f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.4f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                drawWarning(impact);
                if (ticks >= warningTicks) {
                    triggerImpact(player, impact);
                    cancel();
                    return;
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Location resolveTargetLocation(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), maxRange
        );
        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }
        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(maxRange);
        return player.getEyeLocation().add(direction);
    }

    private void drawWarning(Location center) {
        int points = 24;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            Location point = center.clone().add(
                    impactRadius * Math.cos(angle), 0.1, impactRadius * Math.sin(angle)
            );
            point.getWorld().spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(255, 80, 20), 1.3f));
        }
        center.getWorld().spawnParticle(Particle.LAVA, center.clone().add(0, 0.4, 0), 3, 0.4, 0.2, 0.4, 0);
        center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 8, 0), 6, 0.2, 1.5, 0.2, 0.01);
    }

    private void triggerImpact(Player caster, Location impactLocation) {
        impactLocation.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 6, 0.8, 0.3, 0.8, 0);
        impactLocation.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impactLocation, 20, 1.2, 0.4, 1.2, 0.02);
        MatchSoundUtils.play(config, caster, Sound.ENTITY_GENERIC_EXPLODE, 1.6f, 0.6f);
        MatchSoundUtils.play(config, caster, Sound.ENTITY_IRON_GOLEM_DEATH, 0.8f, 0.5f);

        for (PlacedStructure structure : demolitionService.findStructuresInRadius(impactLocation, impactRadius)) {
            if (demolitionService.isEnemyStructure(caster, structure)) {
                demolitionService.demolish(structure, impactLocation);
            }
        }

        for (Entity nearby : impactLocation.getWorld().getNearbyEntities(
                impactLocation, impactRadius, impactRadius, impactRadius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) continue;
            if (!teamManager.isEnemy(caster, target)) continue;
            if (target.getLocation().distanceSquared(impactLocation) > impactRadius * impactRadius) continue;

            combatService.applyAbilityDamage(caster, target, damage, getName(), impactLocation);

            Vector direction = target.getLocation().toVector().subtract(impactLocation.toVector());
            if (direction.lengthSquared() == 0) {
                direction = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
            }
            Vector knockback = direction.normalize().multiply(knockbackStrength);
            knockback.setY(0.9);
            target.setVelocity(knockback);
        }
    }
}
