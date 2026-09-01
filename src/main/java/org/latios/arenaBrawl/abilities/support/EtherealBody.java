package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class EtherealBody implements Ability {

    private final long durationMillis;
    private final long cooldownMs;

    private final AbilityCost cost;
    private final EtherealBodyManager etherealBodyManager;

    public EtherealBody(CooldownManager cooldownManager, EtherealBodyManager etherealBodyManager,
                        CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.durationMillis = config.getLong("duration-millis", 4000L);
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);

        this.cost = new CooldownCost(cooldownManager, "ethereal_body", cooldownMs, combatUpgradeManager);
        this.etherealBodyManager = etherealBodyManager;
    }

    @Override
    public String getName() { return "Ethereal Body"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Converts your body into an ethereal state, healing you for the total amount received afterwards.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Effect", "Absorbs damage & heals back"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        etherealBodyManager.activate(player, durationMillis);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1.0f, 1.2f);

        player.getWorld().spawnParticle(
                Particle.DUST,
                player.getLocation().add(0, 1.0, 0),
                40,
                0.5, 0.8, 0.5,
                new Particle.DustOptions(Color.fromRGB(180, 255, 255), 1.5f)
        );

        return true;
    }
}