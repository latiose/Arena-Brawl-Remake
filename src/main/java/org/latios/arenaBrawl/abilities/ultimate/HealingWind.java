
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
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HealingWind implements Ability {

    private static final double PUSH_RADIUS = 6.0;
    private static final double PUSH_STRENGTH = 2.2;
    private static final double HEAL_RADIUS = 10.0;
    private static final double HEAL_PER_SECOND = 50.0;
    private static final long HEAL_DURATION_MILLIS = 6_000;
    private static final long CHARGE_TIME_MILLIS = 60_000;
    private final CooldownManager cooldownManager;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public HealingWind(CooldownManager cooldownManager, UsageManager usageManager,
                       TeamManager teamManager, PlayerHealthManager healthManager) {
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
        cooldownManager.setCooldown(player, "healingwind", CHARGE_TIME_MILLIS);
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Push Radius", String.valueOf(PUSH_RADIUS)),
                new AbilityStat("Heal Radius", String.valueOf(HEAL_RADIUS)),
                new AbilityStat("Heal per second", String.valueOf(HEAL_PER_SECOND)),
                new AbilityStat("Duration", "6s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        for (Entity nearby : center.getWorld().getNearbyEntities(center, PUSH_RADIUS, PUSH_RADIUS, PUSH_RADIUS)) {
            if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                Vector push = target.getLocation().toVector().subtract(center.toVector());
                push.setY(0);
                if (push.lengthSquared() < 0.0001) push = new Vector(1, 0, 0);
                push.normalize().multiply(PUSH_STRENGTH);
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
                if (!player.isOnline() || elapsedMillis >= HEAL_DURATION_MILLIS) {
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
        healthManager.heal(player, HEAL_PER_SECOND);
        player.sendMessage(MessageUtils.positive()+String.format("§3Your Healing Wind healed you for §a%d §3health!", (int) HEAL_PER_SECOND));
        for (Entity nearby : player.getNearbyEntities(HEAL_RADIUS, HEAL_RADIUS, HEAL_RADIUS)) {
            if (nearby instanceof Player ally && teamManager.isAlly(player, ally) && !healed.contains(ally)) {
                healthManager.heal(ally, HEAL_PER_SECOND);
                player.sendMessage(MessageUtils.positive()+String.format("§3Your Healing Wind healed §a" + ally.getName() + " §3for §a%d §3health!", (int) HEAL_PER_SECOND));
                ally.sendMessage(MessageUtils.positive()+String.format("§e%s§3's Healing Wild healed you for §a%d §3health!",
                        player.getName(), (int) HEAL_PER_SECOND));
                healed.add(ally);
            }
        }
    }
}