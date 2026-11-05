package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.cost.OffensiveEnergyDiscount;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;

import java.util.List;


public class SkillManipulation implements Ability {

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final long chargeTimeMillis;
    private final long durationMillis;
    private final double discountPercent;

    public SkillManipulation(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                             AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.durationMillis = config.getLong("duration-ms", 10000L);
        this.discountPercent = config.getDouble("discount-percent", 0.50);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.cost = new UltimateCost(cooldownManager, usageManager, "skillmanipulation");
    }

    @Override
    public String getName() {
        return "Skill Manipulation";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "skillmanipulation", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        OffensiveEnergyDiscount.activate(player, discountPercent, durationMillis);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.ENCHANTED_HIT,
                player.getLocation().add(0, 1, 0), 40, 0.4, 0.7, 0.4, 0.3);
        player.getWorld().spawnParticle(Particle.WITCH,
                player.getLocation().add(0, 1, 0), 30, 0.4, 0.7, 0.4, 0.05);

        return true;
    }

    @Override
    public String getDescription() {
        return "Cuts the amount of energy required to perform an offensive skill by "
                + Math.round(discountPercent * 100.0) + "% for " + (durationMillis / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Energy Discount", Math.round(discountPercent * 100.0) + "%"),
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
