package org.latios.arenaBrawl.runes;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.EnergyManager;


import java.util.Random;

public class RuneManager {

    private final EnergyManager energyManager;
    private final RuneSelectionManager selectionManager;
    private final RuneConfigManager runeConfigManager;
    private final Random random = new Random();
    private final DebuffManager debuffManager;
    public RuneManager(EnergyManager energyManager, RuneSelectionManager selectionManager, DebuffManager debuffManager,RuneConfigManager runeConfigManager) {
        this.energyManager = energyManager;
        this.selectionManager = selectionManager;
        this.runeConfigManager = runeConfigManager;
        this.debuffManager = debuffManager;
    }

    /**
     * Rolls the proc chance for whichever rune the attacker has equipped.
     * Returns the damage multiplier to apply to this hit (>1.0 if Rune of Damage procced, 1.0 otherwise).
     */
    public double tryProc(Player attacker, Player victim) {
        RuneType equipped = selectionManager.getSelection(attacker);
        RuneConfig config = runeConfigManager.get(equipped.getConfigId());

        double procChance = config.getDouble("proc-chance", getDefaultProcChance(equipped));

        if (random.nextDouble() >= procChance) {
            return 1.0; // did not proc
        }

        return applyRune(equipped, config, attacker, victim);
    }

    private double applyRune(RuneType rune, RuneConfig config, Player attacker, Player victim) {

        switch (rune) {
            case SPEED -> {
                long durationTicks = config.getLong("duration-ticks", 60);
                int amplifier = config.getInt("amplifier", 2);

                attacker.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, (int) durationTicks, amplifier, true, false
                ));
                attacker.sendMessage("§eYour §f" + rune.getDisplayName() + " §ewas activated!");
                return 1.0;
            }
            case SLOW -> {
                long durationTicks = config.getLong("duration-ticks", 3000);
                debuffManager.tryApply(victim, DebuffType.SLOW, durationTicks);
                attacker.sendMessage("§eYour §5" + rune.getDisplayName() + " §ewas activated!");
                return 1.0;
            }
            case DAMAGE -> {
                double multiplier = config.getDouble("damage-multiplier", 2.0);
                attacker.sendMessage("§eYour §c" + rune.getDisplayName() + " §ewas activated!");
                return multiplier;
            }
            case ENERGY -> {
                double amount = config.getDouble("energy-amount", 10.0);
                energyManager.addEnergy(attacker, amount);
                attacker.sendMessage("§eYour §e" + rune.getDisplayName() + " §ewas activated!");
                return 1.0;
            }
            default -> {
                return 1.0;
            }
        }
    }

    private double getDefaultProcChance(RuneType rune) {
        return switch (rune) {
            case SPEED -> 0.2;
            case SLOW, ENERGY -> 0.15;
            case DAMAGE -> 0.48;
        };
    }
}