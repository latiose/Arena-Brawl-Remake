package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class DarkPassage implements Ability {

    private static final double MAX_RANGE = 12.0;
    private static final long DURATION_TICKS = 100L; //
    private static final long COOLDOWN_MS = 30_000;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final Plugin plugin;

    public DarkPassage(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager, CombatUpgradeManager combatUpgradeManager) {
        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "darkpassage", COOLDOWN_MS, combatUpgradeManager);
        this.teamManager = teamManager;
    }

    @Override
    public String getName() { return "Dark Passage"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Block targetBlock = player.getTargetBlockExact((int) MAX_RANGE);
        if (targetBlock == null) {
            player.sendMessage("§eTarget location too far or invalid!");
            return false;
        }

        Location lanternLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5);
        player.getWorld().playSound(lanternLoc, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 0.8f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= DURATION_TICKS || !player.isOnline() || player.isDead()) {
                    lanternLoc.getWorld().spawnParticle(Particle.SMOKE, lanternLoc, 15, 0.2, 0.2, 0.2, 0.05);
                    cancel();
                    return;
                }

                lanternLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, lanternLoc, 3, 0.1, 0.1, 0.1, 0.01);
                lanternLoc.getWorld().spawnParticle(Particle.SOUL, lanternLoc, 1, 0.1, 0.1, 0.1, 0.02);

                for (Player ally : lanternLoc.getWorld().getPlayers()) {
                    if (ally.equals(player)) continue;
                    if (!teamManager.isAlly(player, ally)) continue;

                    if (ally.getLocation().distanceSquared(lanternLoc) <= 2.25) {
                        Location destination = player.getLocation();
                        ally.teleport(destination);

                        ally.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                        ally.getWorld().spawnParticle(Particle.PORTAL, destination.add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Places a soul lantern. If an ally steps on it, they instantly teleport to your position.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", "5s"),
                new AbilityStat("Range", (int) MAX_RANGE + "m"),
                new AbilityStat("Cooldown", (COOLDOWN_MS / 1000) + "s")
        );
    }
}