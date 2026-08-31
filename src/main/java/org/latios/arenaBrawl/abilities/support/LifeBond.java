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

    private static final double RANGE = 25.0;
    private static final double HEAL_PER_SECOND = 25.0;
    private static final int DURATION_SECONDS = 8;
    private static final long COOLDOWN_MS = 30_000;

    public static final Map<UUID, UUID> ACTIVE_BONDS = new HashMap<>();

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;
    private final Plugin plugin;

    public LifeBond(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                    PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager) {
        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "lifebond", COOLDOWN_MS, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() { return "Life Bond"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player targetAlly = AbilityTargeting.findAllyAlongRay(player, teamManager, RANGE);

        if (targetAlly == null) {
            player.sendMessage("§eThere is not valid player within range!");
            return false;
        }

        final Player ally = targetAlly;
        ACTIVE_BONDS.put(ally.getUniqueId(), player.getUniqueId());

        player.sendMessage(MessageUtils.positive() + String.format("§3Linked §a%s §3with Life Bond!", ally.getName()));
        ally.sendMessage(MessageUtils.positive() + String.format("§a%s §3linked Life Bond with you!", player.getName()));

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.2f);
        ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.2f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (ticksElapsed >= DURATION_SECONDS * 20 || !player.isOnline() || !ally.isOnline() || player.isDead() || ally.isDead()) {
                    ACTIVE_BONDS.remove(ally.getUniqueId());
                    cancel();
                    return;
                }

                if (player.getLocation().distance(ally.getLocation()) <= RANGE) {
                    drawBondLine(player.getLocation().add(0, 1.0, 0), ally.getLocation().add(0, 1.0, 0));

                    if (ticksElapsed % 20 == 0) {
                        healthManager.heal(player, HEAL_PER_SECOND);
                        healthManager.heal(ally, HEAL_PER_SECOND);
                        player.sendMessage(MessageUtils.positive() + String.format("§3Your Life Bond healed you for §a%d §3health!", (int) HEAL_PER_SECOND));
                        ally.sendMessage(MessageUtils.positive() + String.format("§a%s§3's Life Bond healed you for §a%d §3health!", player.getName(), (int) HEAL_PER_SECOND));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void drawBondLine(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.WAX_ON, point, 1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getDescription() {
        return "Binds with a targeted ally, redirecting some of their taken damage to you while healing both per second.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal/sec", (int) HEAL_PER_SECOND + " HP"),
                new AbilityStat("Redirect Damage", "30%"),
                new AbilityStat("Duration", DURATION_SECONDS + "s"),
                new AbilityStat("Cooldown", (COOLDOWN_MS / 1000) + "s")
        );
    }
}