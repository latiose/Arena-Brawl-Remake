package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Swap implements Ability {

    private final int maxRange;
    private final long cooldownMillis;
    private final long stunDurationMillis;
    private final long teleportDelayTicks;

    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public Swap(CooldownManager cooldownManager, DebuffManager debuffManager,
                CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.maxRange = config.getInt("max-range", 30);
        this.cooldownMillis = config.getLong("cooldown-ms", 30_000L);
        this.stunDurationMillis = config.getLong("stun-duration-ms", 2_000L);
        this.teleportDelayTicks = config.getLong("teleport-delay-ticks", 40L);

        this.cost = new CooldownCost(cooldownManager, "swap", cooldownMillis, combatUpgradeManager);
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() {
        return "Swap";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findTargetAlongRay(player, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        debuffManager.tryApply(target, DebuffType.STUN, stunDurationMillis);
        debuffManager.tryApply(player, DebuffType.STUN, stunDurationMillis);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !target.isOnline()) {
                    return;
                }

                Location playerLoc = player.getLocation();
                Location targetLoc = target.getLocation();

                Location newPlayerLoc = targetLoc.clone();
                newPlayerLoc.setYaw(playerLoc.getYaw());
                newPlayerLoc.setPitch(playerLoc.getPitch());

                Location newTargetLoc = playerLoc.clone();
                newTargetLoc.setYaw(targetLoc.getYaw());
                newTargetLoc.setPitch(targetLoc.getPitch());

                player.teleport(newPlayerLoc);
                target.teleport(newTargetLoc);

                player.getWorld().playSound(newPlayerLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                target.getWorld().playSound(newTargetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            }
        }.runTaskLater(ArenaBrawlPlugin.getInstance(), teleportDelayTicks);

        return true;
    }

    @Override
    public String getDescription() {
        return "Stuns yourself and an ally/enemy then swap places";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMillis / 1000L) + "s"),
                new AbilityStat("Range", maxRange + " blocks"),
                new AbilityStat("Bonus", "Stun (" + (stunDurationMillis / 1000L) + "s) before teleport")
        );
    }
}