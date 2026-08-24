package org.latios.arenaBrawl.abilities.support;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.support.OrbitShieldManager;
import org.latios.arenaBrawl.abilities.support.OrbitShieldType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

public class StarShield implements Ability {

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public StarShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "starshield", 45000,combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.STAR_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.STAR_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.STAR_SHIELD.getActivationParticle(), player.getLocation(), 25, 0.5, 1, 0.5
        );
        return true;
    }
}