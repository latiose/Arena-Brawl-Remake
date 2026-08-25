package org.latios.arenaBrawl.abilities.support;

import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class HolyWater implements Ability {

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final DebuffManager debuffManager;
    private static final double SELF_HEAL = 300;
    private static final double ALLY_HEAL = 50;

    public HolyWater(CooldownManager cooldownManager, TeamManager teamManager, PlayerHealthManager healthManager, DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager) {
        this.cost = new CooldownCost(cooldownManager, "holywater", 30000,combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Holy Water"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player closestAlly = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity nearby : player.getNearbyEntities(6, 4, 6)) {
            if (nearby instanceof Player nearbyPlayer && teamManager.isAlly(player, nearbyPlayer)) {
                double distance = nearbyPlayer.getLocation().distanceSquared(player.getLocation());
                if (distance < closestDistance && nearbyPlayer.getGameMode()!= GameMode.SPECTATOR) {
                    closestDistance = distance;
                    closestAlly = nearbyPlayer;
                }
            }
        }

        healthManager.heal(player, SELF_HEAL);
        debuffManager.clear(player);
        player.sendMessage(MessageUtils.positive()+String.format("§3Your Holy Water healed you for §a%d §3health!", (int) SELF_HEAL));

        if (closestAlly != null) {
            healthManager.heal(closestAlly, ALLY_HEAL);
            debuffManager.clear(closestAlly);

            player.sendMessage(MessageUtils.positive()+String.format("§3Your Holy Water healed §e%s §3for §a%d §3health!",
                    closestAlly.getName(), (int) ALLY_HEAL));
            closestAlly.sendMessage(MessageUtils.positive()+String.format("§e%s§3's Holy Water healed you for §a%d §3health!",
                    player.getName(), (int) ALLY_HEAL));
        }

        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 50);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        return true;
    }

    @Override
    public String getDescription() {
        return "Heals and cleanses user and nearby allies";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Self Heal", (int) SELF_HEAL + " HP"),
                new AbilityStat("Ally Heal", (int) ALLY_HEAL + " HP"),
                new AbilityStat("Cooldown", "30s"),
                new AbilityStat("Bonus", "Cleanses debuffs")
        );
    }
}