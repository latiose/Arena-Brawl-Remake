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
    private final double healPerEnergy;
    private final double maxHeal;
    private final AbilityCost cost;
    private final EnergyManager energyManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Overcharge(EnergyManager energyManager, PlayerHealthManager healthManager, AbilityConfig config) {
        this.minEnergy = config.getDouble("min-energy", 25.0);
        this.healPerEnergy = config.getDouble("heal-per-energy", 6.0);
        this.maxHeal = config.getDouble("max-heal", 600.0);

        this.cost = new ConsumeAllEnergyCost(energyManager, minEnergy);
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
        double energy = energyManager.getEnergy(player);
        if (energy <= 0.0) return false;

        double heal = Math.min(maxHeal, energy * healPerEnergy);
        healthManager.heal(player, heal, getName());

        player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 1.0);
        player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);

        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        player.sendMessage("§bOvercharge §7converted §e" + (int) energy + " §7energy into health!");
        return true;
    }

    @Override
    public String getDescription() {
        return "Dumps all of your energy at once, converting every point into health. The more energy you have stored, the bigger the heal.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal per Energy", String.valueOf((int) healPerEnergy)),
                new AbilityStat("Maximum Heal", String.valueOf((int) maxHeal)),
                new AbilityStat("Minimum Energy", String.valueOf((int) minEnergy))
        );
    }

    /** Requires a minimum amount of energy, but on activation drains every point the player has. */
    private static final class ConsumeAllEnergyCost extends EnergyCost {

        private final EnergyManager energyManager;
        private final double minEnergy;

        ConsumeAllEnergyCost(EnergyManager energyManager, double minEnergy) {
            super(energyManager, minEnergy);
            this.energyManager = energyManager;
            this.minEnergy = minEnergy;
        }

        @Override
        public void pay(Player player) {
            energyManager.consume(player, energyManager.getEnergy(player));
        }

        @Override
        public String getBaseCostDescription() {
            return "All energy (min " + (int) minEnergy + ")";
        }
    }
}
