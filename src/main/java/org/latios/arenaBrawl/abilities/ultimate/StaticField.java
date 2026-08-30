package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class StaticField implements Ability {

    private static final double RADIUS = 5.0;
    private static final double DAMAGE = 300.0;
    private static final long SILENCE_DURATION_MS = 5_000;
    private static final long CHARGE_TIME_MILLIS = 60_000;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final CooldownManager cooldownManager;

    public StaticField(CooldownManager cooldownManager, TeamManager teamManager,
                       CombatService combatService, DebuffManager debuffManager, UsageManager usageManager) {
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "staticfield");
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "staticfield", CHARGE_TIME_MILLIS);
    }

    @Override
    public String getName() { return "Static Field"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation();

        center.getWorld().playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.8f);

        int points = 36;
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;

            Location particleLoc = center.clone().add(x, 0.2, z);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
        }

        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 1, 0), 40, 1.5, 0.5, 1.5, 0.1);

        for (Player enemy : center.getWorld().getPlayers()) {
            if (!teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

            if (enemy.getLocation().distance(center) <= RADIUS) {
                combatService.applyAbilityDamage(player, enemy, DAMAGE, getName());

                debuffManager.tryApply(enemy, DebuffType.SILENCE, SILENCE_DURATION_MS);

                enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.5f);
                enemy.getWorld().spawnParticle(Particle.CRIT, enemy.getLocation().add(0, 1.0, 0), 15, 0.3, 0.3, 0.3, 0.1);
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        return "Emits an electric discharge around you, dealing damage and silencing nearby enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Silence Duration", (SILENCE_DURATION_MS / 1000) + "s"),
                new AbilityStat("Radius", (int) RADIUS + "m"),
                new AbilityStat("Cooldown", (CHARGE_TIME_MILLIS / 1000) + "s")
        );
    }
}