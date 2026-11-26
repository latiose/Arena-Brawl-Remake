package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.runes.RuneManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class LuckyStrikes implements Ability {

    private final long durationMs;
    private final double procMultiplier;
    private final long cooldownMs;

    private final AbilityCost cost;
    private final RuneManager runeManager;

    public LuckyStrikes(CooldownManager cooldownManager, RuneManager runeManager,
                        CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.procMultiplier = config.getDouble("proc-multiplier", 1.5);
        this.cooldownMs = config.getLong("cooldown-ms", 40000L);

        this.cost = new CooldownCost(cooldownManager, "luckystrikes", cooldownMs, combatUpgradeManager);
        this.runeManager = runeManager;
    }

    @Override
    public String getName() {
        return "Lucky Strikes";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        runeManager.grantLuckyStrikes(player, durationMs, procMultiplier);

        Location center = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.DUST, center, 30, 0.5, 0.7, 0.5, 0.0,
                new Particle.DustOptions(Color.fromRGB(255, 210, 70), 1.4f));
        player.getWorld().spawnParticle(Particle.END_ROD, center, 15, 0.4, 0.6, 0.4, 0.03);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.6f);

        return true;
    }

    @Override
    public String getDescription() {
        return "Multiplies your rune's activation chance by " + formatMultiplier()
                + " for " + (durationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Rune Chance", "x" + formatMultiplier()),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    private String formatMultiplier() {
        if (procMultiplier == Math.floor(procMultiplier)) {
            return String.valueOf((int) procMultiplier);
        }
        return String.valueOf(procMultiplier);
    }

    public static void clear(Player player) {
        RuneManager.clearLuckyStrikeStatic(player);
    }
}
