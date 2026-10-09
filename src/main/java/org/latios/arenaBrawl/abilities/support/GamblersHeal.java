package org.latios.arenaBrawl.abilities.support;

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
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GamblersHeal implements Ability {

    private final long cooldownMs;
    private final double healLower;
    private final double healHigher;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public GamblersHeal(CooldownManager cooldownManager, PlayerHealthManager healthManager,
                        CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.healLower = config.getDouble("heal-lower", 100.0);
        this.healHigher = config.getDouble("heal-higher", 500.0);
        this.cost = new CooldownCost(cooldownManager, "gamblersheal", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Gambler's Heal";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        double randomHeal = ThreadLocalRandom.current().nextDouble(healLower, healHigher + 1.0);

        healthManager.heal(player, randomHeal, getName());

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.2);
        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        MatchSoundUtils.play(config, player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

        return true;
    }

    @Override
    public String getDescription() {
        return "Heals the user for a random amount between " + (int) healLower + " and " + (int) healHigher + " HP.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal Range", (int) healLower + " - " + (int) healHigher + " HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Bonus", "Random Roll")
        );
    }
}