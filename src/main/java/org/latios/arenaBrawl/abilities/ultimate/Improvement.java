package org.latios.arenaBrawl.abilities.ultimate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.AbilityCostReduction;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Improvement implements Ability {

    private static final AbilitySlot[] IMPROVABLE_SLOTS = {
            AbilitySlot.OFFENSIVE,
            AbilitySlot.UTILITY,
            AbilitySlot.SUPPORT
    };

    private final long chargeTimeMillis;
    private final double offensiveDiscount;
    private final double utilityDiscount;
    private final double supportDiscount;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;

    public Improvement(CooldownManager cooldownManager, UsageManager usageManager, AbilityConfig config) {
        this.cooldownManager = cooldownManager;
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.offensiveDiscount = config.getDouble("offensive-discount", 0.20);
        this.utilityDiscount = config.getDouble("utility-discount", 0.20);
        this.supportDiscount = config.getDouble("support-discount", 0.20);
        this.cost = new UltimateCost(cooldownManager, usageManager, "improvement");
    }

    @Override
    public String getName() {
        return "Improvement";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "improvement", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Randomly improves one of your offensive, utility or support abilities, reducing its cost.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Improved Ability", "Random"),
                new AbilityStat("Offensive Cost", "-" + Math.round(offensiveDiscount * 100) + "%"),
                new AbilityStat("Utility Cost", "-" + Math.round(utilityDiscount * 100) + "%"),
                new AbilityStat("Support Cost", "-" + Math.round(supportDiscount * 100) + "%"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        AbilitySlot slot = IMPROVABLE_SLOTS[
                ThreadLocalRandom.current().nextInt(IMPROVABLE_SLOTS.length)];
        double discount = getDiscount(slot);

        AbilityCostReduction.apply(player, slot, discount);

        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 10.0f, 1.4f);
        player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 8.0f, 1.6f);
        player.getWorld().spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.0, 0),
                80, 0.6, 1.0, 0.6, 0.5);
        player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1.0, 0),
                25, 0.4, 0.8, 0.4, 0.05);

        player.sendMessage(Component.text("Your ", NamedTextColor.YELLOW)
                .append(Component.text("IMPROVEMENT", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text(" empowered your ", NamedTextColor.YELLOW))
                .append(Component.text(formatSlot(slot), NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" ability! Its cost is reduced by ", NamedTextColor.YELLOW))
                .append(Component.text(Math.round(discount * 100) + "%", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("!", NamedTextColor.YELLOW)));

        return true;
    }

    private double getDiscount(AbilitySlot slot) {
        return switch (slot) {
            case OFFENSIVE -> offensiveDiscount;
            case UTILITY -> utilityDiscount;
            case SUPPORT -> supportDiscount;
            default -> 0.0;
        };
    }

    private String formatSlot(AbilitySlot slot) {
        return switch (slot) {
            case OFFENSIVE -> "offensive";
            case UTILITY -> "utility";
            case SUPPORT -> "support";
            default -> "unknown";
        };
    }
}
