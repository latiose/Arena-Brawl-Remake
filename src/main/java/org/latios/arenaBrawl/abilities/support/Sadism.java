package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Sadism implements Ability {

    private final long cooldownMs;
    private final long windowMs;

    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;

    public Sadism(CooldownManager cooldownManager, PlayerHealthManager healthManager,
                  CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.windowMs = config.getLong("window-ms", 3000L);
        this.cost = new CooldownCost(cooldownManager, "sadism", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
    }

    @Override
    public String getName() {
        return "Sadism";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Feeds on your pain: heals back all the damage you took over the last "
                + (windowMs / 1000.0) + " seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage Window", (windowMs / 1000.0) + "s"),
                new AbilityStat("Healing", "All damage taken"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        double recentDamage = healthManager.getRecentDamageTaken(player, windowMs);

        healthManager.heal(player, recentDamage, getName());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0f, 0.7f);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                player.getLocation().add(0, 1.4, 0), 15, 0.4, 0.5, 0.4, 0.0);
        player.getWorld().spawnParticle(Particle.HEART,
                player.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.05);

        return true;
    }
}
