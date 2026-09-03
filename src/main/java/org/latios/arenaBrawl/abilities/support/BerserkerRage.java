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
import org.latios.arenaBrawl.powerups.DamageBuffManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class BerserkerRage implements Ability {

    private final double selfDamage;
    private final double damageIncrease;
    private final long durationMs;
    private final long cooldownMs;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final DamageBuffManager damageBuffManager;

    public BerserkerRage(PlayerHealthManager healthManager,
                         DamageBuffManager damageBuffManager, CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager,
                         AbilityConfig config) {
        this.selfDamage = config.getDouble("self-damage", 50.0);
        this.damageIncrease = config.getDouble("damage-increase", 1.20);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.cooldownMs = config.getLong("cooldown-ms", 0L);
        this.cost = new CooldownCost(cooldownManager, "berserkerrage", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.damageBuffManager = damageBuffManager;
    }

    @Override
    public String getName() {
        return "Berserker Rage";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Inflicts 50 self-damage to grant 20% bonus damage for 5 seconds. Does not stack.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Self Damage", (int) selfDamage + " HP"),
                new AbilityStat("Damage Boost", (int) (damageIncrease * 100) + "%"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", "None")
        );
    }

    @Override
    public boolean activate(Player player) {
        healthManager.damage(player, selfDamage);
        player.sendMessage(MessageUtils.negative() + String.format(
                "§3Your Berserker Rage hit you §3for §c%d §3damage.",
                (int) selfDamage
        ));
        damageBuffManager.applyBuff(player, damageIncrease, durationMs,"Berserker Rage");

        player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.4, 0.3, 0.0);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 20, 0.2, 0.5, 0.2, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);

        return true;
    }
}