package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Lifts a targeted player (enemy or ally look-target) into a spinning vortex
 * for a short duration. The victim receives a full Stun (cannot move or use
 * abilities) and a 100% damage-reduction shield (cannot be damaged), meaning
 * both teams lose access to one fighter for the duration.
 */
public class Cyclone implements Ability {

    private final double range;
    private final long durationMs;
    private final long cooldownMs;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Cyclone(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                   ShieldManager shieldManager, DebuffManager debuffManager,
                   CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.debuffManager = debuffManager;

        this.range = config.getDouble("range", 15.0);
        this.durationMs = config.getLong("duration-ms", 3000L);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);

        this.cost = new CooldownCost(cooldownManager, "cyclone", cooldownMs, combatUpgradeManager);
        this.config = config;
    }

    @Override
    public String getName() {
        return "Cyclone";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Lifts the player you are looking at (enemy or ally) into a vortex. The victim cannot act, move, or be damaged for " + (durationMs / 1000L) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Range", (int) range + "m"),
                new AbilityStat("Immunity", "100% damage"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findTargetAlongRay(player, range);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        shieldManager.applyShield(target, 1.0, durationMs, "Cyclone");

        boolean enemy = teamManager.isEnemy(player, target);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 10.0f, 1.4f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 10.0f, 1.6f);
        target.getWorld().spawnParticle(Particle.PORTAL, target.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.4);

        if (enemy) {
            target.sendMessage("§cYou are trapped in a §bCyclone§c!");
        } else {
            target.sendMessage("§bA Cyclone§3 shields you from all harm!");
        }

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= durationMs / 50L || !target.isOnline() || target.isDead()) {
                    cancel();
                    return;
                }

                drawTornado(target.getLocation());
                if (ticksElapsed % 6 == 0) {
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 0.7f, 1.8f);
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void drawTornado(Location base) {
        double time = System.currentTimeMillis() / 1000.0;

        for (double h = 0.0; h <= 2.5; h += 0.25) {
            double progress = h / 2.5;
            double angle = time * 4.0 + progress * 6.0;
            double radius = 1.4 - progress * 1.1;

            Location point = base.clone().add(Math.cos(angle) * radius, h, Math.sin(angle) * radius);
            point.getWorld().spawnParticle(Particle.DUST, point, 1,
                    new Particle.DustOptions(Color.fromRGB(170, 230, 255), 1.4f));

            if (progress < 0.3) {
                point.getWorld().spawnParticle(Particle.CLOUD, point, 1, 0, 0, 0, 0.01);
            }
        }
    }
}
