package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class ShadowStep implements Ability {

    private final int maxRange;
    private final long cooldownMillis;
    private final int speedAmplifier;
    private final int speedDurationTicks;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityCost cost;
    private final TeamManager teamManager;

    public ShadowStep(CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config, SpeedBuffManager speedBuffManager) {
        this.maxRange = config.getInt("max-range", 20);
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 2);
        this.speedDurationTicks = config.getInt("speed-duration-ticks", 40);

        this.cost = new CooldownCost(cooldownManager, "shadowstep", cooldownMillis, combatUpgradeManager);
        this.teamManager = teamManager;
        this.speedBuffManager = speedBuffManager;
    }

    @Override
    public String getName() { return "Shadow Step"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Location teleportLocation = AbilityTargeting.findBehindLocation(target);

        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.3, 0.5, 0.3);
        player.teleport(teleportLocation);
        player.getWorld().spawnParticle(Particle.SMOKE, teleportLocation, 20, 0.3, 0.5, 0.3);

        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationTicks * 50L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Teleports behind the nearest enemy in your crosshair. Grants Speed after teleporting";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Range", maxRange + " blocks"),
                new AbilityStat("Bonus", "Speed III (" + (speedDurationTicks / 20) + "s) after teleport")
        );
    }
}