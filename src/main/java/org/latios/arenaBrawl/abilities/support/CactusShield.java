package org.latios.arenaBrawl.abilities.support;


import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class CactusShield implements Ability {

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public CactusShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "cactusshield", 30000, combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.CACTUS_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.CACTUS_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.CACTUS_SHIELD.getActivationParticle(), player.getLocation(), 20, 0.5, 1, 0.5
        );
        return true;
    }

    @Override
    public String getDescription() {
        return "Blocks the 4 next attacks and deals damage to the attacker";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", "0"),
                new AbilityStat("Cooldown", "30s"),
                new AbilityStat("Bonus", "Blocks the 4 next enemy attacks"),
                new AbilityStat("Damage", "50")
        );
    }
}
