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
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Rewind implements Ability {

    public static final Map<UUID, UUID> ACTIVE_REWUNDS = new HashMap<>();

    private final long durationTicks;
    private final long chargeTimeMillis;
    private final double reviveHealth;
    private final AbilityConfig config;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final Plugin plugin;

    // teamManager ya no se usa (no hay que buscar aliados); se mantiene en la firma
    // para no tener que tocar el registro de la habilidad.
    public Rewind(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                  UsageManager usageManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownManager = cooldownManager;

        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.reviveHealth = config.getDouble("revive-health", 400.0);

        this.cost = new UltimateCost(cooldownManager, usageManager, "rewind");
        this.config = config;
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
        final UUID playerUuid = player.getUniqueId();
        ACTIVE_REWUNDS.put(playerUuid, playerUuid);

        MatchSoundUtils.play(config, player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2f, 1.8f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.5f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                try {
                    ticksElapsed += 2;

                    if (ticksElapsed >= durationTicks || !player.isOnline() || player.isDead()
                            || !ACTIVE_REWUNDS.containsKey(playerUuid)) {
                        cancel();
                        return;
                    }

                    Location headLoc = player.getLocation().add(0, 2.3, 0);
                    double angle = (ticksElapsed * 15) % 360;
                    double rad = Math.toRadians(angle);
                    double x = Math.cos(rad) * 0.6;
                    double z = Math.sin(rad) * 0.6;

                    headLoc.getWorld().spawnParticle(Particle.WAX_OFF, headLoc.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                    headLoc.getWorld().spawnParticle(Particle.END_ROD, headLoc.clone().add(-x, 0, -z), 1, 0, 0, 0, 0);

                    if (ticksElapsed % 10 == 0) {
                        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 0.5f, 1.8f);
                    }
                } catch (Exception e) {
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                ACTIVE_REWUNDS.remove(playerUuid);
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
        return "Places a temporal mark on yourself for " + (durationTicks / 20) + "s. If you take lethal damage, you will be revived with some HP.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Revive Health", (int) reviveHealth + " HP"),
                new AbilityStat("Duration", (durationTicks / 20) + "s"),
                new AbilityStat("Target", "Self"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000) + "s")
        );
    }
}