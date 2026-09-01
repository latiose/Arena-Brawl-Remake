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

public class StarShield implements Ability {

    private final long cooldownMs;

    private final AbilityCost cost;
    private final OrbitShieldManager orbitShieldManager;

    public StarShield(CooldownManager cooldownManager, OrbitShieldManager orbitShieldManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 45000L);
        this.cost = new CooldownCost(cooldownManager, "starshield", cooldownMs, combatUpgradeManager);
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

    @Override
    public String getDescription() {
        return "Blocks the 3 next attacks, on hit heals the user and applies a random debuff to the attacker.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) OrbitShieldType.STAR_SHIELD.getHealPerCharge() + " HP"),
                new AbilityStat("Charges", String.valueOf(OrbitShieldType.STAR_SHIELD.getChargeCount())),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Bonus", "Debuffs attackers with slow, immobilization or stun for 4s each.")
        );
    }
}