package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;

public class BoneShield implements Ability {

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public BoneShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager) {
        this.cost = new CooldownCost(cooldownManager, "boneshield", 30000);
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
}
