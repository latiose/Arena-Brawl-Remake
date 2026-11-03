package org.latios.arenaBrawl.abilities.support;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class AbsorptionShield implements Ability {

    private final long cooldownMs;
    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public AbsorptionShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager,
                            CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.cost = new CooldownCost(cooldownManager, "absorptionshield", cooldownMs, combatUpgradeManager);
        this.orbitShieldManager = orbitShieldManager;
    }

    @Override
    public String getName() { return OrbitShieldType.ABSORPTION_SHIELD.getDisplayName(); }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        orbitShieldManager.activate(player, OrbitShieldType.ABSORPTION_SHIELD);
        player.getWorld().spawnParticle(
                OrbitShieldType.ABSORPTION_SHIELD.getActivationParticle(), player.getLocation(), 20, 0.5, 1, 0.5
        );
        return true;
    }

    @Override
    public String getDescription() {
        return "Blocks the " + OrbitShieldType.ABSORPTION_SHIELD.getChargeCount()
                + " next attacks; each blocked hit restores "
                + (int) OrbitShieldType.ABSORPTION_SHIELD.getEnergyPerCharge() + " energy.";
    }

    @Override
    public List<AbilityStat> getStats() {
        OrbitShieldType type = OrbitShieldType.ABSORPTION_SHIELD;
        return List.of(
                new AbilityStat("Energy", (int) type.getEnergyPerCharge() + " per blocked hit"),
                new AbilityStat("Charges", String.valueOf(type.getChargeCount())),
                new AbilityStat("Duration", (type.getDurationMillis() / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
