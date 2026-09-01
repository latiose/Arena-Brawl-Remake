package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LifeBond implements Ability {

    public static final Map<UUID, UUID> ACTIVE_BONDS = new HashMap<>();

    private final double range;
    private final double healPerSecond;
    private final int durationSeconds;
    private final long cooldownMs;
    private final double redirectPercent;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final Plugin plugin;

    public LifeBond(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                    PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager,
                    AbilityConfig config) {
        this.plugin = plugin;
        this.range = config.getDouble("range", 25.0);
        this.healPerSecond = config.getDouble("heal-per-second", 25.0);
        this.durationSeconds = config.getInt("duration-seconds", 8);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.redirectPercent = config.getDouble("redirect-percent", 0.30);

        this.cost = new CooldownCost(cooldownManager, "lifebond", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Life Bond"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player targetAlly = AbilityTargeting.findAllyAlongRay(player, teamManager, range);

        if (targetAlly == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        final Player ally = targetAlly;
        ACTIVE_BONDS.put(ally.getUniqueId(), player.getUniqueId());

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.2f);
        ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.2f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                try {
                    ticksElapsed += 2;

                    if (ticksElapsed >= durationSeconds * 20 || !player.isOnline() || !ally.isOnline() || player.isDead() || ally.isDead()) {
                        cancel();
                        return;
                    }

                    if (player.getLocation().distance(ally.getLocation()) <= range) {
                        drawBondLine(player.getLocation().add(0, 1.0, 0), ally.getLocation().add(0, 1.0, 0));

                        if (ticksElapsed % 20 == 0) {
                            healthManager.heal(player, healPerSecond, getName());
                            healthManager.healAlly(player, ally, healPerSecond, getName());
                        }
                    }
                } catch (Exception e) {
                    cancel();
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                ACTIVE_BONDS.remove(ally.getUniqueId());
                super.cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void drawBondLine(Location start, Location end) {
        Vector dir = end.toVector().subtract(start.toVector());
        double length = dir.length();
        if (length == 0) return;

        dir.multiply(0.5 / length);
        Location current = start.clone();
        int steps = (int) (length / 0.5);

        for (int i = 0; i < steps; i++) {
            current.add(dir);
            current.getWorld().spawnParticle(Particle.WAX_ON, current, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getDescription() {
        return "Binds with a targeted ally, redirecting some of their taken damage to you while healing both per second.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal/sec", (int) healPerSecond + " HP"),
                new AbilityStat("Redirect Damage", (int) (redirectPercent * 100) + "%"),
                new AbilityStat("Duration", durationSeconds + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}