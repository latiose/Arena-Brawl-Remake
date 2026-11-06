package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.DamageVulnerabilityManager;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;


public class AdrenalineSurge implements Ability {

    private final long durationMs;
    private final double vulnerability;
    private final long cooldownMs;
    private final double energyRecoverPercent;

    private final AbilityCost cost;
    private final EnergyManager energyManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;
    private final AbilityConfig config;

    public AdrenalineSurge(CooldownManager cooldownManager, EnergyManager energyManager,
                           DamageVulnerabilityManager damageVulnerabilityManager,
                           CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.energyManager = energyManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;

        this.durationMs = config.getLong("duration-ms", 5000L);
        this.vulnerability = config.getDouble("vulnerability-percent", 25.0) / 100.0;
        this.cooldownMs = config.getLong("cooldown-ms", 15000L);
        this.energyRecoverPercent = config.getDouble("energy-recover-percent", 75.0);

        this.cost = new CooldownCost(cooldownManager, "adrenalinesurge", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Adrenaline Surge";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Instantly recovers " + (int) energyRecoverPercent + "% of your energy bar, but you take "
                + (int) (vulnerability * 100) + "% extra damage for " + (durationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Energy", "+" + (int) energyRecoverPercent + "% of bar"),
                new AbilityStat("Vulnerability", "+" + (int) (vulnerability * 100) + "% damage taken"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        double maxEnergy = energyManager.getMaxEnergy(player);
        double missing = Math.max(0.0, maxEnergy - energyManager.getEnergy(player));

        if (missing < 1.0) {
            player.sendMessage("§cYour energy is already full!");
            return false;
        }

        double gain = maxEnergy * (energyRecoverPercent / 100.0);
        energyManager.addEnergy(player, gain);
        damageVulnerabilityManager.applyVulnerability(player, vulnerability, durationMs, "Adrenaline Surge");

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_WARDEN_HEARTBEAT, 2.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.DUST, loc.clone().add(0, 1, 0), 40, 0.4, 0.6, 0.4, 0.05,
                new Particle.DustOptions(Color.fromRGB(220, 30, 30), 1.6f));
        player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);


        return true;
    }
}
