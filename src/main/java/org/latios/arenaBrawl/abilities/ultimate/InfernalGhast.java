package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Summons an immortal ghast that stays high in the sky and fires 50-damage
 * fireballs at a single enemy for the rest of the match. It starts on the
 * enemy the caster is aiming at, or the nearest one, and only switches target
 * when its current target dies. Movement and shooting use the vanilla ghast
 * AI (normal sounds included).
 */
public class InfernalGhast implements Ability {

    private final long chargeTimeMillis;
    private final int maxRange;
    private final double fireballDamage;
    private final double hoverHeight;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final InfernalGhastEntityManager entityManager;

    public InfernalGhast(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                         InfernalGhastEntityManager entityManager, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.maxRange = config.getInt("max-range", 80);
        this.fireballDamage = config.getDouble("fireball-damage", 50.0);
        this.hoverHeight = config.getDouble("hover-height", 10.0);

        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "infernalghast");
        this.teamManager = teamManager;
        this.entityManager = entityManager;
    }

    @Override
    public String getName() {
        return "Infernal Ghast";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "infernalghast", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;

        // Aimed enemy first, nearest enemy as fallback (handled by the manager).
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);
        entityManager.summonGhast(player, target);
        return true;
    }

    @Override
    public String getDescription() {
        return "Summons an immortal ghast that hunts one enemy for the rest of the match using normal ghast "
                + "AI. Its fireballs deal " + (int) fireballDamage + " damage, cannot be reflected, and it only "
                + "picks a new target when the current one dies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Fireball Damage", (int) fireballDamage + " HP"),
                new AbilityStat("Fire Rate", "Vanilla ghast"),
                new AbilityStat("Flight Height", (int) hoverHeight + "m"),
                new AbilityStat("Duration", "Rest of the match"),
                new AbilityStat("Charge Time", (chargeTimeMillis / 1000L) + "s")
        );
    }
}
