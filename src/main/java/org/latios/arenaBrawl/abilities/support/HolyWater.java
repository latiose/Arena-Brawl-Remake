package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class HolyWater implements Ability {

    private final long cooldownMs;
    private final double selfHeal;
    private final double allyHeal;
    private final double radius;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final DebuffManager debuffManager;

    public HolyWater(CooldownManager cooldownManager, TeamManager teamManager, PlayerHealthManager healthManager,
                     DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.selfHeal = config.getDouble("self-heal", 300.0);
        this.allyHeal = config.getDouble("ally-heal", 50.0);
        this.radius = config.getDouble("radius", 6.0);

        this.cost = new CooldownCost(cooldownManager, "holywater", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Holy Water"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player closestAlly = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player nearbyPlayer && teamManager.isAlly(player, nearbyPlayer)) {
                double distance = nearbyPlayer.getLocation().distanceSquared(player.getLocation());
                if (distance < closestDistance && nearbyPlayer.getGameMode() != GameMode.SPECTATOR) {
                    closestDistance = distance;
                    closestAlly = nearbyPlayer;
                }
            }
        }

        healthManager.heal(player, selfHeal, getName());
        debuffManager.clear(player);

        if (closestAlly != null) {
            healthManager.healAlly(player, closestAlly, allyHeal, getName());
            debuffManager.clear(closestAlly);
        }

        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        return true;
    }

    @Override
    public String getDescription() {
        return "Heals and cleanses user and nearby allies";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Self Heal", (int) selfHeal + " HP"),
                new AbilityStat("Ally Heal", (int) allyHeal + " HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Bonus", "Cleanses debuffs")
        );
    }
}