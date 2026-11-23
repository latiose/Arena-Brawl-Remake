package org.latios.arenaBrawl.runes;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.*;

import java.util.List;
import java.util.Random;

public class RuneManager {

    private final EnergyManager energyManager;
    private final RuneSelectionManager selectionManager;
    private final RuneConfigManager runeConfigManager;
    private final Random random = new Random();
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final PlayerHealthManager healthManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;
    private CombatService combatService;
    private MatchManager matchManager;
    public RuneManager(EnergyManager energyManager, RuneSelectionManager selectionManager, DebuffManager debuffManager, RuneConfigManager runeConfigManager, SpeedBuffManager speedBuffManager, PlayerHealthManager healthManager, DamageVulnerabilityManager damageVulnerabilityManager,
                       CombatService combatService) {
        this.combatService = combatService;
        this.energyManager = energyManager;
        this.selectionManager = selectionManager;
        this.runeConfigManager = runeConfigManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
        this.healthManager = healthManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;
    }

    /**
     * Rolls the proc chance for whichever rune the attacker has equipped.
     * Returns the damage multiplier to apply to this hit (>1.0 if Rune of Damage procced, 1.0 otherwise).
     */
    public double tryProc(Player attacker, Player victim) {
        RuneType equipped = selectionManager.getSelection(attacker);
        // Rune of Thorns only reacts to being hit, it never procs on attack.
        if (equipped == RuneType.THORNS) {
            return 1.0;
        }
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
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
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
            case WEAKNESS -> {
                long durationMillis = config.getLong("duration-ticks", 4000);
                double bonusMultiplier = config.getDouble("bonus-multiplier", 0.30);
                damageVulnerabilityManager.applyVulnerability(victim, bonusMultiplier, durationMillis, rune.getDisplayName());
                attacker.sendMessage("§eYour §6" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case HUNGER -> {
                long durationMillis = config.getLong("duration-ms", 5000L);
                HungerImmunity.apply(attacker, durationMillis);
                attacker.setFoodLevel(HungerManager.MAX_HUNGER);
                attacker.sendMessage("§eYour §6" + rune.getDisplayName() + " §ewas activated!");
                attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                return 1.0;
            }
            case STARVATION -> {
                int hungerBars = config.getInt("hunger-bars", 3);
                long noRegenMillis = config.getLong("no-regen-ms", 5000L);
                if (victim != null) {
                    // Song of Power applies HungerImmunity, so this single check covers both buffs.
                    boolean immune = HungerImmunity.isActive(victim);
                    if (!immune && hungerBars > 0) {
                        // Removes the bars and blocks regeneration so the loss isn't undone next tick.
                        HungerSuppression.starve(victim, hungerBars, noRegenMillis);
                    }
                    attacker.sendMessage("§eYour §8" + rune.getDisplayName() + " §ewas activated!");
                    attacker.playSound(attacker.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);
                }
                return 1.0;
            }
            case ONESHOT -> {
                if (victim != null) {
                    List<Player> matchPlayers = getMatchPlayers(attacker);
                    for (Player online : matchPlayers) {
                        if (!online.isOnline()) continue;
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

    /**
     * Rolls the victim's Rune of Thorns when they take melee damage and
     * reflects a percentage of that damage back to the attacker.
     */
    public void onMeleeDamageTaken(Player victim, Player attacker, double damageTaken) {
        if (attacker == null || victim == null || attacker.equals(victim) || damageTaken <= 0) return;

        RuneType equipped = selectionManager.getSelection(victim);
        if (equipped != RuneType.THORNS) return;

        RuneConfig config = runeConfigManager.get(equipped.getConfigId());
        double procChance = config.getDouble("proc-chance", getDefaultProcChance(equipped));
        if (random.nextDouble() >= procChance) return;

        double reflectPercent = config.getDouble("reflect-percent", 20.0);
        double reflected = damageTaken * (reflectPercent / 100.0);
        if (reflected <= 0) return;


        combatService.applyAbilityDamage(victim, attacker, reflected, "Rune of Thorns", attacker.getLocation());
        victim.sendMessage("§eYour §4" + equipped.getDisplayName() + " §ewas activated!");
        victim.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_INFECT, 1.0f, 1.0f);

    }


    private double getDefaultProcChance(RuneType rune) {
        return switch (rune) {
            case SPEED -> 0.2;
            case SLOW, ENERGY, HEALING, THORNS, WEAKNESS, HUNGER, STARVATION -> 0.15;
            case DAMAGE -> 0.48;
            case CONFUSION -> 0.20;
            case IMMOBILIZE -> 0.01;
            case ONESHOT -> 0.0001; // 0.01%
        };


    }


    public void setCombatService(CombatService combatService) {
        this.combatService = combatService;
    }

    public void setMatchManager(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    /**
     * Only the players inside the attacker's match should hear or see rune
     * effects. Falls back to just the attacker when the attacker is not in
     * a match (e.g. during lobby testing), never to every player online.
     */
    private List<Player> getMatchPlayers(Player attacker) {
        if (matchManager != null) {
            List<Player> players = matchManager.getMatchPlayers(attacker);
            if (!players.isEmpty()) {
                return players;
            }
        }
        return List.of(attacker);
    }
}