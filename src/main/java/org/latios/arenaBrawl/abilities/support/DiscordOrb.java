
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
import org.latios.arenaBrawl.general.DamageVulnerabilityManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class DiscordOrb implements Ability {

    private static final long COOLDOWN_MS = 30_000;
    private static final int DURATION_SECONDS = 7;
    private static final double DAMAGE_BONUS = 0.30; // +30% incoming damage
    private static final double MAX_RANGE = 20.0;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;

    public DiscordOrb(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                      CombatUpgradeManager combatUpgradeManager, DamageVulnerabilityManager damageVulnerabilityManager) {
        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "discordorb", COOLDOWN_MS, combatUpgradeManager);
        this.teamManager = teamManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;
    }

    @Override
    public String getName() { return "Discord Orb"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, MAX_RANGE);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        drawInstantBeam(player.getEyeLocation().subtract(0, 0.2, 0), target.getEyeLocation());
        applyDiscord(player, target);

        return true;
    }

    private void drawInstantBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.WITCH, point, 1, 0, 0, 0, 0);
            point.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0, 0, 0, 0);
        }
    }

    private void applyDiscord(Player caster, Player target) {
        damageVulnerabilityManager.applyVulnerability(target, DAMAGE_BONUS, DURATION_SECONDS * 1000L, getName());

        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.8f, 1.8f);
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 1.5f);

        caster.sendMessage(MessageUtils.positive() + String.format("§3Marked %s §3with §5Discord Orb§3!", target.getName()));
        target.sendMessage(MessageUtils.negative() + String.format("§3%s afflicted you with §5Discord Orb§3!", caster.getName()));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!damageVulnerabilityManager.isActive(target) || !target.isOnline() || target.isDead()) {
                    cancel();
                    return;
                }

                Location headLoc = target.getLocation().add(0, 2.2, 0);
                headLoc.getWorld().spawnParticle(Particle.WITCH, headLoc, 4, 0.2, 0.2, 0.2, 0.02);
                headLoc.getWorld().spawnParticle(Particle.SMOKE, headLoc, 2, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    @Override
    public String getDescription() {
        return "Instantly marks an enemy in your line of sight. The marked target receives 30% additional damage for its duration.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Extra Damage", "+30%"),
                new AbilityStat("Duration", DURATION_SECONDS + "s"),
                new AbilityStat("Range", (int) MAX_RANGE + "m"),
                new AbilityStat("Cooldown", (COOLDOWN_MS / 1000) + "s")
        );
    }
}