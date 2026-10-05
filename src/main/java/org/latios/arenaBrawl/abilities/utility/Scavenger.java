package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Scavenger implements Ability {

    private final double energyPerHit;
    private final AbilityCost cost;
    private final ScavengerManager scavengerManager;
    private final long durationMillis;
    private long cooldownMs;
    private final AbilityConfig config;
    public Scavenger(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     ScavengerManager scavengerManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 40000L);
        this.durationMillis = config.getLong("duration-ms", 4000L);
        this.energyPerHit = config.getDouble("energy-per-hit", 10.0);
        this.cost = new CooldownCost(cooldownManager, "scavenger", cooldownMs, upgradeManager);
        this.scavengerManager = scavengerManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Scavenger";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "For " + (durationMillis / 1000) + " seconds, every melee hit you land grants " + (int) energyPerHit + " energy.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Energy per hit", String.valueOf((int) energyPerHit)),
                new AbilityStat("Duration", (durationMillis / 1000) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        scavengerManager.activate(player, durationMillis);
        MatchSoundUtils.play(config, player,Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.5f);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        return true;
    }
}