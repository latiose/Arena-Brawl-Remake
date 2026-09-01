package org.latios.arenaBrawl.abilities.support;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.OrbitShieldType;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SpongeShield implements Ability {

    private final long cooldownMs;

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public SpongeShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager,
                        CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 45000L);
        this.cost = new CooldownCost(cooldownManager, "spongeshield", cooldownMs, combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.SPONGE_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.SPONGE_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.SPONGE_SHIELD.getActivationParticle(), player.getLocation(), 20, 0.5, 1, 0.5
        );
        return true;
    }

    @Override
    public String getDescription() {
        return "Blocks the 3 next attacks, on hit knocksback attacker.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", "0 HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Bonus", "Blocks the 3 next enemy attacks")
        );
    }
}