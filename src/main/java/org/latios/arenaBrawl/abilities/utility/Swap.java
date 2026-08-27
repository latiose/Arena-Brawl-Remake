package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Swap implements Ability {

    private static final int MAX_RANGE = 30;
    private static final long COOLDOWN_MILLIS = 30_000;
    private static final long STUN_DURATION_MILLIS = 2_000;
    private static final long TELEPORT_DELAY_TICKS = 40L;

    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public Swap(CooldownManager cooldownManager, DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "swap", COOLDOWN_MILLIS, combatUpgradeManager);
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
        Player target = AbilityTargeting.findTargetAlongRay(player, MAX_RANGE);

        if (target == null) {
            player.sendMessage("§eThere is no valid player within range!");
            return false;
        }

        debuffManager.tryApply(target, DebuffType.STUN, STUN_DURATION_MILLIS);
        debuffManager.tryApply(player, DebuffType.STUN, STUN_DURATION_MILLIS);

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
        }.runTaskLater(ArenaBrawlPlugin.getInstance(), TELEPORT_DELAY_TICKS);

        return true;
    }

    @Override
    public String getDescription() {
        return "Stuns yourself and an ally/enemy then swap places";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (COOLDOWN_MILLIS / 1000) + "s"),
                new AbilityStat("Range", MAX_RANGE + " blocks"),
                new AbilityStat("Bonus", "Stun (2s) before teleport")
        );
    }
}