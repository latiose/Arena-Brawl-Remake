package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;

import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Rewind implements Ability {

    private static final double RANGE = 14.0;
    private static final long DURATION_TICKS = 100L;
    private static final long CHARGE_TIME_MILLIS = 60_000;
    public static final double REVIVE_HEALTH = 400.0;

    public static final Map<UUID, UUID> ACTIVE_REWUNDS = new HashMap<>();

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CooldownManager cooldownManager;
    private final Plugin plugin;

    public Rewind(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager, UsageManager usageManager) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "rewind");
        this.teamManager = teamManager;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "rewind", CHARGE_TIME_MILLIS);
    }

    @Override
    public String getName() { return "Rewind"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player recipient = AbilityTargeting.findAllyAlongRay(player, teamManager, RANGE);


        if(recipient==null){
            player.sendMessage("§eThere is not valid player within range!");
            return false;
        }
        ACTIVE_REWUNDS.put(recipient.getUniqueId(), player.getUniqueId());

        player.sendMessage(MessageUtils.positive() + String.format("§3Placed §eRewind §3on §a%s§3!", recipient.getName()));
        recipient.sendMessage(MessageUtils.positive() + String.format("§a%s §3cast §eRewind §3on you!", player.getName()));

        recipient.getWorld().playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 1.8f);
        recipient.getWorld().playSound(recipient.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= DURATION_TICKS || !recipient.isOnline() || recipient.isDead() || !ACTIVE_REWUNDS.containsKey(recipient.getUniqueId())) {
                    ACTIVE_REWUNDS.remove(recipient.getUniqueId());
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
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Places a temporal mark on an ally for 5s. If the target takes lethal damage, they will be revived with some HP.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Revive Health", (int) REVIVE_HEALTH + " HP"),
                new AbilityStat("Duration", "5s"),
                new AbilityStat("Range", (int) RANGE + "m"),
                new AbilityStat("Charge Time", (CHARGE_TIME_MILLIS / 1000) + "s")
        );
    }
}