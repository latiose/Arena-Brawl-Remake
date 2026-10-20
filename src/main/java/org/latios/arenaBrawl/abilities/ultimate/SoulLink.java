package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Ultimate that links the two enemy players in a 2v2. While linked, part of
 * any damage taken by one of them is reflected onto the other.
 */
public class SoulLink implements Ability {

    private final long chargeTimeMillis;
    private final long durationMs;
    private final double reflectPercent;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final SoulLinkManager soulLinkManager;
    private final AbilityConfig config;

    public SoulLink(CooldownManager cooldownManager, UsageManager usageManager,
                    TeamManager teamManager, SoulLinkManager soulLinkManager,
                    AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.reflectPercent = config.getDouble("reflect-percent", 0.30);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "soullink");
        this.teamManager = teamManager;
        this.soulLinkManager = soulLinkManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Soul Link";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "soullink", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        List<Player> enemies = new ArrayList<>();

        for (Player candidate : config.getMatchManager().getMatchPlayers(player)) {
            if (candidate.equals(player)
                    || candidate.isDead()
                    || candidate.getGameMode() == GameMode.SPECTATOR
                    || !teamManager.isEnemy(player, candidate)) {
                continue;
            }
            enemies.add(candidate);
        }

        if (enemies.size() < 2) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        Player first = enemies.get(0);
        Player second = enemies.get(1);
        soulLinkManager.link(first, second, durationMs);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.6f);
        first.getWorld().playSound(first.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 1.2f, 0.8f);
        second.getWorld().playSound(second.getLocation(), Sound.BLOCK_SOUL_SAND_BREAK, 1.2f, 0.8f);
        first.getWorld().spawnParticle(
                Particle.SOUL, first.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.02
        );
        second.getWorld().spawnParticle(
                Particle.SOUL, second.getLocation().add(0, 1, 0), 25, 0.5, 0.7, 0.5, 0.02
        );

        player.sendMessage(MessageUtils.positive() + "§3You linked §c" + first.getName()
                + " §3and §c" + second.getName() + "§3!");
        first.sendMessage(MessageUtils.negative() + "§3Your soul is linked to §c"
                + second.getName() + "§3!");
        second.sendMessage(MessageUtils.negative() + "§3Your soul is linked to §c"
                + first.getName() + "§3!");

        return true;
    }

    @Override
    public String getDescription() {
        return "Links both enemies for " + (durationMs / 1000L) +
                "s. Whenever one takes damage, the other receives " +
                (int) (reflectPercent * 100) + "% of that damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Reflected Damage", (int) (reflectPercent * 100) + "%"),
                new AbilityStat("Targets", "Both enemies")
        );
    }
}
