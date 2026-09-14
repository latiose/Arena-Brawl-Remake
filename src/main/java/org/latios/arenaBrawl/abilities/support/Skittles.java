package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.Random;

public class Skittles implements Ability {

    public enum SkittleColor {
        RED("Red"),
        BLUE("Blue"),
        YELLOW("Yellow"),
        ORANGE("Orange");

        private final String displayName;

        SkittleColor(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final DamageBuffManager damageBuffManager;
    private final EnergyManager energyManager;
    private final Random random = new Random();
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final double redHeal;
    private final double redEnergy;
    private final double blueHeal;
    private final long blueSlowDurationTicks;
    private final double yellowHeal;
    private final long yellowSpeedDurationTicks;
    private final double orangeHeal;
    private final double orangeDamageBonus;
    private final long orangeDamageDurationMillis;

    public Skittles(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                           PlayerHealthManager healthManager, DamageBuffManager damageBuffManager,
                           EnergyManager energyManager, DebuffManager debuffManager, SpeedBuffManager speedBuffManager, AbilityConfig config) {
        long cooldownMs = config.getLong("cooldown-ms", 40000L);
        this.cost = new CooldownCost(cooldownManager, "skittles", cooldownMs, upgradeManager);
        this.healthManager = healthManager;
        this.damageBuffManager = damageBuffManager;
        this.energyManager = energyManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
        this.redHeal = config.getDouble("red-heal", 200.0);
        this.redEnergy = config.getDouble("red-energy", 30.0);
        this.blueHeal = config.getDouble("blue-heal", 400.0);
        this.blueSlowDurationTicks = config.getLong("blue-slow-duration-ticks", 4000L);
        this.yellowHeal = config.getDouble("yellow-heal", 300.0);
        this.yellowSpeedDurationTicks = config.getLong("yellow-speed-duration-ticks", 4000L);
        this.orangeHeal = config.getDouble("orange-heal", 200.0);
        this.orangeDamageBonus = config.getDouble("orange-damage-bonus", 0.25);
        this.orangeDamageDurationMillis = config.getLong("orange-damage-duration-millis", 5000L);
    }

    @Override
    public String getName() {
        return "Skittles";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Eat a random Skittle for a random effect! Red heals and grants energy, Blue heals "
                + "heavily but slows you, Yellow heals and grants speed, Orange heals and boosts your damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Red", (int) redHeal + " HP + " + (int) redEnergy + " energy"),
                new AbilityStat("Blue", (int) blueHeal + " HP, Slowness " + (blueSlowDurationTicks / 1000) + "s"),
                new AbilityStat("Yellow", (int) yellowHeal + " HP, Speed II " + (yellowSpeedDurationTicks / 20) + "s"),
                new AbilityStat("Orange", (int) orangeHeal + " HP, +" + (int) (orangeDamageBonus * 100) + "% damage "
                        + (orangeDamageDurationMillis / 1000) + "s"),
                new AbilityStat("Odds", "25% each")
        );
    }

    @Override
    public boolean activate(Player player) {
        SkittleColor color = rollColor();
        applyEffect(player, color);
        playEatFeedback(player, color);

        player.sendMessage("§3You ate a " + colorCode(color) + color.getDisplayName() + " §3Skittle!");
        return true;
    }

    private SkittleColor rollColor() {
        SkittleColor[] colors = SkittleColor.values();
        return colors[random.nextInt(colors.length)];
    }

    private void applyEffect(Player player, SkittleColor color) {
        switch (color) {
            case RED -> {
                healthManager.heal(player, redHeal, "Skittles");
                energyManager.addEnergy(player, redEnergy);
            }
            case BLUE -> {
                healthManager.heal(player, blueHeal, "Skittles");
                debuffManager.tryApply(player, DebuffType.SLOW,blueSlowDurationTicks);
            }
            case YELLOW -> {
                healthManager.heal(player, yellowHeal, "Skittles");
                speedBuffManager.applyBuff(player,1,yellowSpeedDurationTicks);

            }
            case ORANGE -> {
                healthManager.heal(player, orangeHeal, "Skittles");
                damageBuffManager.applyBuff(player, 1.0 + orangeDamageBonus, orangeDamageDurationMillis, "Skittles");
            }
        }
    }

    private void playEatFeedback(Player player, SkittleColor color) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);

        Particle.DustOptions dust = new Particle.DustOptions(getParticleColor(color), 1.2f);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.2, 0), 15, 0.3, 0.3, 0.3, dust);
    }

    private org.bukkit.Color getParticleColor(SkittleColor color) {
        return switch (color) {
            case RED -> org.bukkit.Color.RED;
            case BLUE -> org.bukkit.Color.BLUE;
            case YELLOW -> org.bukkit.Color.YELLOW;
            case ORANGE -> org.bukkit.Color.ORANGE;
        };
    }

    private String colorCode(SkittleColor color) {
        return switch (color) {
            case RED -> "§c";
            case BLUE -> "§9";
            case YELLOW -> "§e";
            case ORANGE -> "§6";
        };
    }
}