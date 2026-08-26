package org.latios.arenaBrawl.abilities.support;


import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class BoneShield implements Ability {

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public BoneShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager,CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "boneshield", 30000, combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.BONE_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.BONE_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.BONE_SHIELD.getActivationParticle(), player.getLocation(), 20, 0.5, 1, 0.5
        );
        return true;
    }

    @Override
    public String getDescription() {
        return "Blocks the 5 next attacks, on hit heals the user for 30 health";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) OrbitShieldType.BONE_SHIELD.getHealPerCharge() * 5 + " HP"),
                new AbilityStat("Cooldown", "30s"),
                new AbilityStat("Bonus", "Blocks the 5 next enemy attacks")
        );
    }
}
