package org.latios.arenaBrawl.abilities.support;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class GamblersShield implements Ability {

    private final long cooldownMs;
    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public GamblersShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager,
                          CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.cost = new CooldownCost(cooldownManager, "gamblersshield", cooldownMs, combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.GAMBLERS_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.GAMBLERS_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.GAMBLERS_SHIELD.getActivationParticle(), player.getLocation(), 20, 0.5, 1, 0.5
        );
        return true;
    }

    @Override
    public String getDescription() {
        return "Blocks the " + OrbitShieldType.GAMBLERS_SHIELD.getChargeCount() + " next attacks, on hit heals applies a random effect from other shields.";
    }

    @Override
    public List<AbilityStat> getStats() {
        int totalHeal = (int) (OrbitShieldType.GAMBLERS_SHIELD.getHealPerCharge() * OrbitShieldType.GAMBLERS_SHIELD.getChargeCount());
        return List.of(
                new AbilityStat("Heal", totalHeal + " HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Bonus", "Blocks the " + OrbitShieldType.GAMBLERS_SHIELD.getChargeCount() + " next enemy attacks")
        );
    }
}