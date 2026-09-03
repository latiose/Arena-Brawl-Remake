package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
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
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NinjaDash implements Ability {

    private final long chargeTimeMillis;
    private final double damage;
    private final double dashDistance;
    private final double hitRadius;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Plugin plugin;

    public NinjaDash(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                     TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.damage = config.getDouble("damage", 300.0);
        this.dashDistance = config.getDouble("dash-distance", 15.0);
        this.hitRadius = config.getDouble("hit-radius", 2.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "ninjadash");
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Ninja Dash";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "ninjadash", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Dash swiftly forward, slicing through enemies in your path and dealing massive damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + " HP"),
                new AbilityStat("Distance", (int) dashDistance + " blocks"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.2f, 0.6f);

        Set<Player> hitEnemies = new HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 5;
            final Vector stepVelocity = direction.clone().multiply(dashDistance / maxTicks);

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    player.setFallDistance(0);
                    cancel();
                    return;
                }

                player.setVelocity(stepVelocity);
                player.setFallDistance(0);

                Location currentLoc = player.getLocation().add(0, 1.0, 0);

                player.getWorld().spawnParticle(Particle.DUST, currentLoc, 15, 0.4, 0.4, 0.4, 0.0,
                        new Particle.DustOptions(Color.fromRGB(50, 255, 50), 1.5f));
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLoc, 2, 0.2, 0.2, 0.2, 0.0);


                for (Player enemy : player.getWorld().getPlayers()) {
                    if (enemy.equals(player)) continue;
                    if (!teamManager.isEnemy(player, enemy)) continue;
                    if (hitEnemies.contains(enemy)) continue;

                    if (enemy.getLocation().add(0, 1.0, 0).distanceSquared(currentLoc) <= (hitRadius * hitRadius)) {
                        hitEnemies.add(enemy);
                        combatService.applyAbilityDamage(player, enemy, damage, getName());
                        enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f);
                        enemy.getWorld().spawnParticle(Particle.CRIT, enemy.getLocation().add(0, 1.0, 0), 20, 0.3, 0.5, 0.3, 0.2);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}