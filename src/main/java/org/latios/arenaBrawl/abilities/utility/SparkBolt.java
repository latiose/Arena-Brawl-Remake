package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SparkBolt implements Ability {

    private final double areaRadius;
    private final long durationMillis;
    private final long cooldownMillis;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final EnergyModifierManager energyModifierManager;

    public SparkBolt(CooldownManager cooldownManager, TeamManager teamManager,
                     CombatUpgradeManager combatUpgradeManager, EnergyModifierManager energyModifierManager,
                     AbilityConfig config) {
        this.durationMillis = config.getLong("duration-ms", 8_000L);
        this.areaRadius = config.getDouble("area-radius", 6.0);
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);

        this.cost = new CooldownCost(cooldownManager, "sparkbolt", cooldownMillis, combatUpgradeManager);
        this.teamManager = teamManager;
        this.energyModifierManager = energyModifierManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Spark Bolt"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        List<Player> targets = player.getNearbyEntities(areaRadius, areaRadius, areaRadius).stream()
                .filter(entity -> entity instanceof Player)
                .map(entity -> (Player) entity)
                .filter(target -> !target.equals(player))
                .filter(target -> target.getGameMode() != GameMode.SPECTATOR)
                .filter(target -> !target.isDead())
                .filter(target -> teamManager.isEnemy(player, target))
                .toList();

        spawnAreaEffect(player);

        if (targets.isEmpty()) {
            return true;
        }

        for (Player target : targets) {
            energyModifierManager.addModifier(target, "spark_bolt", 0.5, durationMillis);
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15);
            player.sendMessage(MessageUtils.positive() + "§3Your Spark Bolt hit §3" + target.getName()
                    + " §3and halved their energy regen!");
            target.sendMessage(MessageUtils.negative() + "§3You were hit by §a" + player.getName()
                    + "§3's Spark Bolt! Energy regen halved for " + (durationMillis / 1000L) + "s.");
        }

        MatchSoundUtils.play(config, player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 2.0f);

        return true;
    }

    private void spawnAreaEffect(Player player) {
        var center = player.getLocation().add(0, 0.15, 0);
        int points = Math.max(24, (int) Math.ceil(areaRadius * 8));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            var point = center.clone().add(
                    Math.cos(angle) * areaRadius,
                    0,
                    Math.sin(angle) * areaRadius
            );
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 2, 0.04, 0.02, 0.04, 0.01);
        }
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0),
                12, 0.35, 0.6, 0.35, 0.04);
    }

    @Override
    public String getDescription() {
        return "Halves an enemy's energy regeneration for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Area Radius", areaRadius + " blocks"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s")
        );
    }
}