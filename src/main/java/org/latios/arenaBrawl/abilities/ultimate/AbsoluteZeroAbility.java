
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AbsoluteZeroAbility implements Ability {

    private static final double RADIUS = 6.0;
    private static final long MAX_CHARGE_TICKS = 100;
    private static final double MAX_DAMAGE = 400.0;
    private static final long CHARGE_TIME_MILLIS = 60_000;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    private final Map<UUID, ChannelTask> activeChannels = new HashMap<>();

    public AbsoluteZeroAbility(CooldownManager cooldownManager, UsageManager usageManager,
                               TeamManager teamManager, CombatService combatService, DebuffManager debuffManager) {
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "absolutezero");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Absolute Zero"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "absolutezero", CHARGE_TIME_MILLIS);
    }


    @Override
    public boolean activate(Player player) {

        ChannelTask task = new ChannelTask(player);
        activeChannels.put(player.getUniqueId(), task);
        task.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        return true;
    }

    @Override
    public String getDescription() {
        return "Channels for 5 seconds, rooting yourself and slowing all nearby enemies "
                + "Deals a big chunk of damage";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Max Damage", String.valueOf(MAX_DAMAGE)),
                new AbilityStat("Max Channel Time", "5s"),
                new AbilityStat("Radius", "6 blocks"),
                new AbilityStat("Charge Time", (CHARGE_TIME_MILLIS / 1000) + "s"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    private class ChannelTask extends BukkitRunnable {

        private final Player player;
        private final Location startLocation;
        private long ticksCharged = 0;

        ChannelTask(Player player) {
            this.player = player;
            this.startLocation = player.getLocation().clone();
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cleanup();
                cancel();
                return;
            }

            if (ticksCharged >= MAX_CHARGE_TICKS) {
                finish();
                return;
            }

            Location current = player.getLocation();
            if (current.getX() != startLocation.getX() || current.getZ() != startLocation.getZ()) {
                startLocation.setYaw(current.getYaw());
                startLocation.setPitch(current.getPitch());
                player.teleport(startLocation);
            }
           debuffManager.tryApply(player,DebuffType.IMMOBILIZE,5000);

            Location center = player.getLocation();
            for (Entity nearby : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
                if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                    debuffManager.tryApply(target, DebuffType.SLOW, 100);
                }
            }

            center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 30, RADIUS / 2, 0.5, RADIUS / 2, 0.02);
            if (ticksCharged % 10 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
            }

            ticksCharged++;
        }

        private void cleanup() {
            activeChannels.remove(player.getUniqueId());
        }

        public void finish() {
            cleanup();
            cancel();

            double chargeRatio = Math.min(1.0, (double) ticksCharged / MAX_CHARGE_TICKS);
            double finalDamage = MAX_DAMAGE * chargeRatio;

            Location center = player.getLocation();
            Set<Player> hitPlayers = new HashSet<>();

            for (Entity nearby : center.getWorld().getNearbyEntities(center, RADIUS, RADIUS, RADIUS)) {
                if (nearby instanceof Player target && teamManager.isEnemy(player, target) && !hitPlayers.contains(target)) {
                    combatService.applyAbilityDamage(player, target, finalDamage, getName());
                    hitPlayers.add(target);
                }
            }

            center.getWorld().spawnParticle(Particle.EXPLOSION, center, 5);
            center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
        }
    }
}