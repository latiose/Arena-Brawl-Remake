
package org.latios.arenaBrawl.runes;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.EnergyManager;


import java.util.Random;

public class RuneManager {

    private static final int SPEED_DURATION_TICKS = 60;
    private static final int SPEED_AMPLIFIER = 2;
    private static final int SLOW_DURATION_TICKS = 3000;
    private static final double ENERGY_AMOUNT = 10.0;

    private final EnergyManager energyManager;
    private final RuneSelectionManager selectionManager;
    private final Random random = new Random();
    private final DebuffManager debuffManager;

    public RuneManager(EnergyManager energyManager, RuneSelectionManager selectionManager,DebuffManager debuffManager) {
        this.energyManager = energyManager;
        this.selectionManager = selectionManager;
        this.debuffManager = debuffManager;
    }

    /**
     * Rolls the proc chance for whichever rune the attacker has equipped.
     * Returns the damage multiplier to apply to this hit (2.0 if Rune of Damage procced, 1.0 otherwise).
     */
    public double tryProc(Player attacker, Player victim) {
        RuneType equipped = selectionManager.getSelection(attacker);

        if (random.nextDouble() >= equipped.getProcChance()) {
            return 1.0; // did not proc
        }

        applyRune(equipped, attacker, victim);
        return equipped == RuneType.DAMAGE ? 2.0 : 1.0;
    }

    private void applyRune(RuneType rune, Player attacker, Player victim) {
        switch (rune) {
            case SPEED -> {
                attacker.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, SPEED_DURATION_TICKS, SPEED_AMPLIFIER, true, false
                ));
                attacker.sendMessage("§eYour §f" + rune.getDisplayName() + " §ewas activated");
            }
            case SLOW -> {
                debuffManager.tryApply(victim, DebuffType.SLOW, SLOW_DURATION_TICKS);
                attacker.sendMessage("§eYour §5" + rune.getDisplayName() + " §ewas activated");
            }
            case DAMAGE -> attacker.sendMessage("§eYour §c" + rune.getDisplayName() + " §ewas activated");
            case ENERGY -> {
                energyManager.addEnergy(attacker, ENERGY_AMOUNT);
                attacker.sendMessage("§eYour §e" + rune.getDisplayName() + " §ewas activated");
            }
        }
    }
}