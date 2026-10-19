package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.List;

public class Overcharge implements Ability {

    private final double minEnergy;
    private final double maxEnergy;
    private final double healPerEnergy;
    private final double maxHeal;
    private final AbilityCost cost;
    private final EnergyManager energyManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Overcharge(EnergyManager energyManager, PlayerHealthManager healthManager, AbilityConfig config) {
        this.minEnergy = config.getDouble("min-energy", 25.0);
        this.maxEnergy = Math.max(1.0, config.getDouble("max-energy", 100.0));
        this.maxHeal = config.getDouble("max-heal", 600.0);
        // Default scales so that spending the full max-energy yields exactly max-heal.
        this.healPerEnergy = config.getDouble("heal-per-energy", maxHeal / maxEnergy);

        this.cost = new ConsumeAllEnergyCost(energyManager, minEnergy, maxEnergy);
        this.energyManager = energyManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Overcharge"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        // Only ever convert up to maxEnergy, so extra max-energy upgrades don't inflate the heal.
        double energyUsed = Math.min(energyManager.getEnergy(player), maxEnergy);
        if (energyUsed <= 0.0) return false;

        double heal = Math.min(maxHeal, energyUsed * healPerEnergy);
        healthManager.heal(player, heal, getName());

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 1.0);
        player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);

        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        player.sendMessage("§bOvercharge §7converted §e" + (int) energyUsed + " §7energy into health!");
        return true;
    }

    @Override
    public String getDescription() {
        return "Dumps up to " + (int) maxEnergy + " of your energy at once, converting every point into health. "
                + "The more energy you have (up to that cap), the bigger the heal.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Max Heal", (int) maxHeal + " at " + (int) maxEnergy + " energy"),
                new AbilityStat("Conversion", trim(healPerEnergy) + " HP per energy"),
                new AbilityStat("Minimum Energy", String.valueOf((int) minEnergy))
        );
    }

    private static String trim(double v) {
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    /** Requires a minimum amount of energy, but on activation drains up to {@code maxEnergy}. */
    private static final class ConsumeAllEnergyCost extends EnergyCost {

        private final EnergyManager energyManager;
        private final double minEnergy;
        private final double maxEnergy;

        ConsumeAllEnergyCost(EnergyManager energyManager, double minEnergy, double maxEnergy) {
            super(energyManager, minEnergy);
            this.energyManager = energyManager;
            this.minEnergy = minEnergy;
            this.maxEnergy = maxEnergy;
        }

        @Override
        public void pay(Player player) {
            energyManager.consume(player, Math.min(energyManager.getEnergy(player), maxEnergy));
        }

        @Override
        public String getBaseCostDescription() {
            return "Up to " + (int) maxEnergy + " energy (min " + (int) minEnergy + ")";
        }
    }
}
