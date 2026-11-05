package org.latios.arenaBrawl.runes;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;

import java.util.Random;

public class RuneManager {

    private final EnergyManager energyManager;
    private final RuneSelectionManager selectionManager;
    private final RuneConfigManager runeConfigManager;
    private final Random random = new Random();
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final PlayerHealthManager healthManager;

    public RuneManager(EnergyManager energyManager, RuneSelectionManager selectionManager, DebuffManager debuffManager, RuneConfigManager runeConfigManager, SpeedBuffManager speedBuffManager, PlayerHealthManager healthManager) {
        this.energyManager = energyManager;
        this.selectionManager = selectionManager;
        this.runeConfigManager = runeConfigManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
        this.healthManager = healthManager;
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

                speedBuffManager.applyBuff(attacker, amplifier, durationTicks * 50L);
                attacker.sendMessage("§eYour §f" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case SLOW -> {
                long durationTicks = config.getLong("duration-ticks", 3000);
                debuffManager.tryApply(victim, DebuffType.SLOW, durationTicks);
                attacker.sendMessage("§eYour §5" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case DAMAGE -> {
                double multiplier = config.getDouble("damage-multiplier", 2.0);
                attacker.sendMessage("§eYour §c" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return multiplier;
            }
            case ENERGY -> {
                double amount = config.getDouble("energy-amount", 10.0);
                energyManager.addEnergy(attacker, amount);
                attacker.sendMessage("§eYour §e" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case IMMOBILIZE -> {
                long durationMillis = config.getLong("duration-ticks", 3000);
                debuffManager.tryApply(victim, DebuffType.IMMOBILIZE, durationMillis);
                attacker.sendMessage("§eYour §a" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 0.5f);
                return 1.0;
            }
            case CONFUSION -> {
                if (victim != null) {
                    int currentSlot = victim.getInventory().getHeldItemSlot();
                    int newSlot;

                    do {
                        newSlot = random.nextInt(4);
                    } while (newSlot == currentSlot);
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                    victim.getInventory().setHeldItemSlot(newSlot);
                    attacker.sendMessage("§eYour §0" + rune.getDisplayName() + " §ewas activated!");
                }
                return 1.0;
            }
            case HEALING -> {
                double healAmount = config.getDouble("heal-amount", 20.0);
                healthManager.heal(attacker, healAmount);
                attacker.sendMessage("§eYour §a" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case ONESHOT -> {
                if (victim != null && !healthManager.isEliminated(victim)) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.playSound(online.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.4f);
                        online.sendMessage("§4§l" + attacker.getName() + " §4activated the §c"
                                + rune.getDisplayName() + "§4§l and One Shot §b" + victim.getName() + "§4§l!");
                    }

                    victim.sendMessage("§4§lYou were One Shot §4by " + attacker.getName() + "!");
                    victim.getWorld().strikeLightningEffect(victim.getLocation());
                    healthManager.damage(victim, healthManager.getHealth(victim), attacker);
                }
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
            case SLOW, ENERGY, HEALING -> 0.15;
            case DAMAGE -> 0.48;
            case CONFUSION -> 0.20;
            case IMMOBILIZE -> 0.01;
            case ONESHOT -> 0.0001; // 0.01%
        };


    }


}