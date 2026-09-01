package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Rewind implements Ability {

    public static final Map<UUID, UUID> ACTIVE_REWUNDS = new HashMap<>();

    private final double range;
    private final long durationTicks;
    private final long chargeTimeMillis;
    private final double reviveHealth;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CooldownManager cooldownManager;
    private final Plugin plugin;

    public Rewind(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                  UsageManager usageManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;

        this.range = config.getDouble("range", 14.0);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.reviveHealth = config.getDouble("revive-health", 400.0);

        this.cost = new UltimateCost(cooldownManager, usageManager, "rewind");
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "rewind", chargeTimeMillis);
    }

    @Override
    public String getName() { return "Rewind"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player recipient = AbilityTargeting.findAllyAlongRay(player, teamManager, range);

        if (recipient == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        final UUID recipientUuid = recipient.getUniqueId();
        ACTIVE_REWUNDS.put(recipientUuid, player.getUniqueId());

        player.sendMessage(MessageUtils.positive() + String.format("§3Placed §eRewind §3on §a%s§3!", recipient.getName()));
        recipient.sendMessage(MessageUtils.positive() + String.format("§a%s §3cast §eRewind §3on you!", player.getName()));

        recipient.getWorld().playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 1.8f);
        recipient.getWorld().playSound(recipient.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                try {
                    ticksElapsed += 2;

                    if (ticksElapsed >= durationTicks || !recipient.isOnline() || recipient.isDead() || !ACTIVE_REWUNDS.containsKey(recipientUuid)) {
                        cancel();
                        return;
                    }

                    Location headLoc = recipient.getLocation().add(0, 2.3, 0);
                    double angle = (ticksElapsed * 15) % 360;
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * 0.6;
                    double z = Math.sin(rad) * 0.6;

                    headLoc.getWorld().spawnParticle(Particle.WAX_OFF, headLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    headLoc.getWorld().spawnParticle(Particle.END_ROD, headLoc.clone().add(-x, 0, -z), 1, 0, 0, 0, 0);

                    if (ticksElapsed % 10 == 0) {
                        recipient.getWorld().playSound(recipient.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 0.5f, 1.8f);
                    }
                } catch (Exception e) {
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                ACTIVE_REWUNDS.remove(recipientUuid);
                super.cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    public double getReviveHealth() {
        return reviveHealth;
    }

    @Override
    public String getDescription() {
        return "Places a temporal mark on an ally for " + (durationTicks / 20) + "s. If the target takes lethal damage, they will be revived with some HP.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Revive Health", (int) reviveHealth + " HP"),
                new AbilityStat("Duration", (durationTicks / 20) + "s"),
                new AbilityStat("Range", (int) range + "m"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s")
        );
    }
}