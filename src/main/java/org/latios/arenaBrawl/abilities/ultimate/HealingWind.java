package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealingWind implements Ability {

    private final double pushRadius;
    private final double pushStrength;
    private final double healRadius;
    private final double healPerSecond;
    private final long healDurationMillis;
    private final long chargeTimeMillis;

    private final CooldownManager cooldownManager;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public HealingWind(CooldownManager cooldownManager, UsageManager usageManager,
                       TeamManager teamManager, PlayerHealthManager healthManager,
                       AbilityConfig config) {
        this.pushRadius = config.getDouble("push-radius", 6.0);
        this.pushStrength = config.getDouble("push-strength", 2.2);
        this.healRadius = config.getDouble("heal-radius", 10.0);
        this.healPerSecond = config.getDouble("heal-per-second", 50.0);
        this.healDurationMillis = config.getLong("heal-duration-millis", 6000L);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "healingwind");
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Healing Wind"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Instantly pushes back nearby enemies, then heals you and allies in a large radius "
                + "healing every second for some time";
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "healingwind", chargeTimeMillis);
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Push Radius", (int) pushRadius + "m"),
                new AbilityStat("Heal Radius", (int) healRadius + "m"),
                new AbilityStat("Heal per second", (int) healPerSecond + " HP"),
                new AbilityStat("Duration", (healDurationMillis / 1000L) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, pushRadius, pushRadius, pushRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                Vector push = target.getLocation().toVector().subtract(center.toVector());
                push.setY(0);
                if (push.lengthSquared() < 0.0001) push = new Vector(1, 0, 0);
                push.normalize().multiply(pushStrength);
                push.setY(0.4);
                target.setVelocity(push);
            }
        }

        center.getWorld().spawnParticle(Particle.CLOUD, center, 60, 3, 1, 3, 0.1);
        center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.5f, 0.6f);

        new BukkitRunnable() {
            long elapsedMillis = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsedMillis >= healDurationMillis) {
                    cancel();
                    return;
                }

                if (elapsedMillis % 1000 == 0) {
                    healNearbyAllies(player);
                }

                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 3, 0.5, 0.5, 0.5);

                elapsedMillis += 50;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void healNearbyAllies(Player player) {
        Set<Player> healed = new HashSet<>();
        healed.add(player);
        healthManager.heal(player, healPerSecond, getName());
        for (Entity nearby : player.getNearbyEntities(healRadius, healRadius, healRadius)) {
            if (nearby instanceof Player ally && teamManager.isAlly(player, ally) && !healed.contains(ally)) {
                healthManager.healAlly(player, ally, healPerSecond, getName());
                healed.add(ally);
            }
        }
    }
}