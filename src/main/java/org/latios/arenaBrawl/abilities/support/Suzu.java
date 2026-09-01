package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Suzu implements Ability {

    private final long cooldownMs;
    private final double healAmount;
    private final long invulnerabilityDurationMs;
    private final double radius;
    private final double maxRange;
    private final double step;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final ShieldManager shieldManager;

    public Suzu(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                PlayerHealthManager healthManager, ShieldManager shieldManager,
                CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.healAmount = config.getDouble("heal-amount", 175.0);
        this.invulnerabilityDurationMs = config.getLong("invulnerability-duration-ms", 1000L);
        this.radius = config.getDouble("radius", 3.0);
        this.maxRange = config.getDouble("max-range", 12.0);
        this.step = config.getDouble("step", 0.6);

        this.cost = new CooldownCost(cooldownManager, "suzu", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.shieldManager = shieldManager;
    }

    @Override
    public String getName() {
        return "Suzu";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_EGG_THROW, 1.0f, 1.5f);

        new BukkitRunnable() {
            private Location currentLoc = startLoc.clone();
            private double distanceTraveled = 0;

            @Override
            public void run() {
                currentLoc.add(direction.clone().multiply(step));
                distanceTraveled += step;

                currentLoc.getWorld().spawnParticle(Particle.FIREWORK, currentLoc, 1, 0.02, 0.02, 0.02, 0.01);
                currentLoc.getWorld().spawnParticle(Particle.END_ROD, currentLoc, 1, 0, 0, 0, 0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    triggerProtection(player, currentLoc);
                    cancel();
                    return;
                }
                for (Player target : currentLoc.getWorld().getPlayers()) {
                    if (target.getGameMode() == GameMode.SPECTATOR || target.isDead()) continue;
                    if (teamManager.isEnemy(player, target)) continue;

                    if (target.getBoundingBox().expand(0.3, 0.3, 0.3).contains(currentLoc.getX(), currentLoc.getY(), currentLoc.getZ())) {
                        triggerProtection(player, currentLoc);
                        cancel();
                        return;
                    }
                }

                if (distanceTraveled >= maxRange) {
                    triggerProtection(player, currentLoc);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void triggerProtection(Player caster, Location center) {
        center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.5f, 1.8f);
        center.getWorld().playSound(center, Sound.ITEM_SHIELD_BLOCK, 1.2f, 1.2f);

        center.getWorld().spawnParticle(Particle.FIREWORK, center, 30, 0.5, 0.5, 0.5, 0.1);
        center.getWorld().spawnParticle(Particle.END_ROD, center, 20, 0.8, 0.8, 0.8, 0.05);

        for (Player ally : center.getWorld().getPlayers()) {
            if (ally.getGameMode() == GameMode.SPECTATOR || ally.isDead()) continue;
            if (teamManager.isEnemy(caster, ally)) continue;

            if (ally.getLocation().distance(center) <= radius) {
                if (ally.equals(caster)) {
                    healthManager.heal(caster, healAmount, getName());
                } else {
                    healthManager.healAlly(caster, ally, healAmount, getName());
                }
                ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1.0, 0), 10, 0.3, 0.5, 0.3, 0);

                shieldManager.applyShield(ally, 1.0, invulnerabilityDurationMs);
            }
        }
    }

    @Override
    public String getDescription() {
        return "Throws a cleansing charm that heals and grants 1 second of invulnerability to all allies in range.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal", (int) healAmount + " HP"),
                new AbilityStat("Invulnerability", (invulnerabilityDurationMs / 1000.0) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}