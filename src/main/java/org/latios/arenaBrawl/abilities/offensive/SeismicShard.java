package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class SeismicShard implements Ability {

    private final double energyCost;
    private final double damage;
    private final long slowDurationMs;
    private final int speedAmplifier;
    private final long speedDurationMs;
    private final double maxRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;

    public SeismicShard(EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, DebuffManager debuffManager,
                        SpeedBuffManager speedBuffManager, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 45.0);
        this.damage = config.getDouble("damage", 80.0);
        this.slowDurationMs = config.getLong("slow-duration-ms", 3000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedDurationMs = config.getLong("speed-duration-ms", 3000L);
        this.maxRange = config.getDouble("max-range", 18.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() {
        return "Seismic Shard";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        drawShard(player, target);

        debuffManager.tryApply(player, target, DebuffType.SLOW, slowDurationMs);
        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationMs, getName());
        combatService.applyAbilityDamage(player, target, damage, getName(), target.getLocation());

        return true;
    }

    private void drawShard(Player caster, Player target) {
        Location start = caster.getEyeLocation();
        Location end = target.getLocation().add(0, 1.0, 0);

        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        if (length > 0) {
            direction.normalize().multiply(0.3);
            Location point = start.clone();
            double traveled = 0;

            while (traveled < length) {
                point.getWorld().spawnParticle(Particle.DUST, point, 1, 0.02, 0.02, 0.02, 0.0,
                        new Particle.DustOptions(Color.fromRGB(140, 140, 150), 1.0f));
                point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0.02, 0.02, 0.02, 0.0);
                point.add(direction);
                traveled += 0.3;
            }
        }

        target.getWorld().spawnParticle(Particle.DUST, end, 15, 0.3, 0.4, 0.3, 0.0,
                new Particle.DustOptions(Color.fromRGB(120, 120, 130), 1.3f));
        target.getWorld().spawnParticle(Particle.CRIT, end, 12, 0.3, 0.4, 0.3, 0.1);

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_STONE_BREAK, 0.9f, 1.5f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.2f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.7f, 1.4f);
    }

    @Override
    public String getDescription() {
        return "Hurls a shard of rock at a single enemy, dealing damage and slowing them for "
                + (slowDurationMs / 1000L) + "s while granting you Speed " + roman(speedAmplifier + 1)
                + " for " + (speedDurationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Slow", (slowDurationMs / 1000L) + "s"),
                new AbilityStat("Speed", "Speed " + roman(speedAmplifier + 1)),
                new AbilityStat("Speed Duration", (speedDurationMs / 1000L) + "s"),
                new AbilityStat("Range", (int) maxRange + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    private static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(level);
        };
    }
}
