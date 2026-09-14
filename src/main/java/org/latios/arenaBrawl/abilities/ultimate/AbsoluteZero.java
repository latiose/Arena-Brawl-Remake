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
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
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

public class AbsoluteZero implements Ability {

    private final double radius;
    private final long maxChargeTicks;
    private final double maxDamage;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;

    private final Map<UUID, ChannelTask> activeChannels = new HashMap<>();

    public AbsoluteZero(CooldownManager cooldownManager, UsageManager usageManager,
                        TeamManager teamManager, CombatService combatService,
                        DebuffManager debuffManager, AbilityConfig config) {
        this.radius = config.getDouble("radius", 5.0);
        this.maxChargeTicks = config.getLong("max-charge-ticks", 100L);
        this.maxDamage = config.getDouble("max-damage", 500.0);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

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
        cooldownManager.setCooldown(player, "absolutezero", chargeTimeMillis);
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
        return "Channels for 5 seconds, rooting yourself and slowing all nearby enemies. "
                + "Deals a big chunk of damage";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Max Damage", String.valueOf((int) maxDamage)),
                new AbilityStat("Max Channel Time", (maxChargeTicks / 20L) + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s"),
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

            if (ticksCharged >= maxChargeTicks) {
                finish();
                return;
            }

            Location current = player.getLocation();
            if (current.getX() != startLocation.getX() || current.getZ() != startLocation.getZ()) {
                startLocation.setYaw(current.getYaw());
                startLocation.setPitch(current.getPitch());
                player.teleport(startLocation);
            }

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 10, false, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 250, false, false, false));

            Location center = player.getLocation();
            for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
                if (nearby instanceof Player target && teamManager.isEnemy(player, target)) {
                    debuffManager.tryApply(target, DebuffType.SLOW, 100);
                }
            }

            center.getWorld().spawnParticle(Particle.SNOWFLAKE, center, 30, radius / 2, 0.5, radius / 2, 0.02);
            if (ticksCharged % 10 == 0) {
                center.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
            }

            ticksCharged++;
        }

        private void cleanup() {
            activeChannels.remove(player.getUniqueId());
            player.removePotionEffect(PotionEffectType.SLOWNESS);
            player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        }

        public void finish() {
            cleanup();
            cancel();

            double chargeRatio = Math.min(1.0, (double) ticksCharged / maxChargeTicks);
            double finalDamage = maxDamage * chargeRatio;

            Location center = player.getLocation();
            Set<Player> hitPlayers = new HashSet<>();

            for (Entity nearby : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
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