// runes/RuneManager.java
package org.latios.arenaBrawl.runes;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.general.EnergyManager;


import java.util.Random;

public class RuneManager {

    private static final int SPEED_DURATION_TICKS = 60;
    private static final int SPEED_AMPLIFIER = 2;
    private static final int SLOW_DURATION_TICKS = 60;
    private static final int SLOW_AMPLIFIER = 1;
    private static final double ENERGY_AMOUNT = 10.0;

    private final EnergyManager energyManager;
    private final RuneSelectionManager selectionManager;
    private final Random random = new Random();

    public RuneManager(EnergyManager energyManager, RuneSelectionManager selectionManager) {
        this.energyManager = energyManager;
        this.selectionManager = selectionManager;
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
                attacker.sendMessage("§b" + rune.getDisplayName() + " §fprocced! Speed III for 3s.");
            }
            case SLOW -> {
                victim.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, SLOW_DURATION_TICKS, SLOW_AMPLIFIER, true, false
                ));
                attacker.sendMessage("§b" + rune.getDisplayName() + " §fprocced on " + victim.getName() + "!");
                victim.sendMessage("§c" + attacker.getName() + "'s " + rune.getDisplayName() + " §fslowed you!");
            }
            case DAMAGE -> attacker.sendMessage("§b" + rune.getDisplayName() + " §fprocced! Double damage this hit.");
            case ENERGY -> {
                energyManager.addEnergy(attacker, ENERGY_AMOUNT);
                attacker.sendMessage("§b" + rune.getDisplayName() + " §fprocced! +10 energy.");
            }
        }
    }
}