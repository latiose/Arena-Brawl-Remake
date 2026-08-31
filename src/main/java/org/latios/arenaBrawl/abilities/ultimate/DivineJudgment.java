
package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.team.TeamManager;


import java.util.List;


public class DivineJudgment implements Ability {

    private static final long DURATION_MILLIS = 5_000;
    private static final long DURATION_TICKS = 100;
    private static final double MAX_RANGE = 20.0;
    private static final double EXPLOSION_DAMAGE = 200.0;
    private static final double EXPLOSION_RADIUS = 3.0;
    private static final long CHARGE_TIME_MILLIS = 60_000;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final ShieldManager shieldManager;
    private final CombatService combatService;
    private final CooldownManager cooldownManager;
    public DivineJudgment(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                          ShieldManager shieldManager, CombatService combatService) {
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
        return "An ally becomes immune to damage for 5 seconds. Afterwards, "
                + "they explode for 200 damage to nearby enemies.";
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "divinejudgment", CHARGE_TIME_MILLIS);
    }
    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Immunity Duration", "5s"),
                new AbilityStat("Ally Radius", String.valueOf(MAX_RANGE)),
                new AbilityStat("Explosion Damage", String.valueOf(EXPLOSION_DAMAGE)),
                new AbilityStat("Explosion Radius", String.valueOf(EXPLOSION_RADIUS)),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findAllyAlongRay(player,teamManager,MAX_RANGE);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }
        shieldManager.applyShield(target, 1.0, DURATION_MILLIS); // 100% damage reduction = immunity
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 30, 0.3, 1, 0.3);
        target.sendMessage("§eYou are protected by Divine Judgment! Go!");

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (ticksElapsed >= DURATION_TICKS) {
                    triggerExplosion(target);
                    cancel();
                    return;
                }
                if(ticksElapsed%20==0) {
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

        for (Entity nearby : center.getWorld().getNearbyEntities(center, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (nearby instanceof Player target && teamManager.isEnemy(caster, target)) {
                combatService.applyAbilityDamage(caster, target, EXPLOSION_DAMAGE, getName());
            }
        }
    }
}