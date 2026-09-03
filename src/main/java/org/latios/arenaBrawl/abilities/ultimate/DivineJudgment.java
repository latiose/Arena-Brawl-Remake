package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class DivineJudgment implements Ability {

    private final long durationMillis;
    private final long durationTicks;
    private final double maxRange;
    private final double explosionDamage;
    private final double explosionRadius;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final CombatService combatService;
    private final CooldownManager cooldownManager;

    public DivineJudgment(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                          ShieldManager shieldManager, CombatService combatService, AbilityConfig config) {
        this.durationMillis = config.getLong("duration-millis", 5000L);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.explosionDamage = config.getDouble("explosion-damage", 200.0);
        this.explosionRadius = config.getDouble("explosion-radius", 3.0);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "divinejudgment");
        this.teamManager = teamManager;
        this.shieldManager = shieldManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Divine Judgment"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "An ally becomes immune to damage for " + (durationMillis / 1000L) + " seconds. Afterwards, "
                + "they explode for " + (int) explosionDamage + " damage to nearby enemies.";
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "divinejudgment", chargeTimeMillis);
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immunity Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Ally Radius", (int) maxRange + "m"),
                new AbilityStat("Explosion Damage", String.valueOf((int) explosionDamage)),
                new AbilityStat("Explosion Radius", (int) explosionRadius + "m"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findAllyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }
        shieldManager.applyShield(target, 1.0, durationMillis,"Divine judgment");
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 30, 0.3, 1, 0.3);
        target.sendMessage("§eYou are protected by Divine Judgment! Go!");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= durationTicks) {
                    triggerExplosion(target);
                    cancel();
                    return;
                }
                if (ticksElapsed % 20 == 0) {
                    player.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0, 1, 0), 30, 0.3, 1, 0.3);
                }
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void triggerExplosion(Player caster) {
        Location center = caster.getLocation();
        center.getWorld().spawnParticle(Particle.EXPLOSION, center, 5);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        for (Entity nearby : center.getWorld().getNearbyEntities(center, explosionRadius, explosionRadius, explosionRadius)) {
            if (nearby instanceof Player target && teamManager.isEnemy(caster, target)) {
                combatService.applyAbilityDamage(caster, target, explosionDamage, getName());
            }
        }
    }
}