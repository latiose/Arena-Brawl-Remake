package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class StaticField implements Ability {

    private final double radius;
    private final double damage;
    private final long silenceDurationMs;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final CooldownManager cooldownManager;

    public StaticField(CooldownManager cooldownManager, TeamManager teamManager,
                       CombatService combatService, DebuffManager debuffManager, UsageManager usageManager,
                       AbilityConfig config) {
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;

        this.radius = config.getDouble("radius", 5.0);
        this.damage = config.getDouble("damage", 300.0);
        this.silenceDurationMs = config.getLong("silence-duration-ms", 5000L);
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "staticfield");
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "staticfield", chargeTimeMillis);
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
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location particleLoc = center.clone().add(x, 0.2, z);
            center.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.1, 0.1, 0.1, 0.05);
        }

        center.getWorld().spawnParticle(Particle.FIREWORK, center.clone().add(0, 1, 0), 40, 1.5, 0.5, 1.5, 0.1);

        for (Player enemy : center.getWorld().getPlayers()) {
            if (!teamManager.isEnemy(player, enemy) || enemy.isDead()) continue;

            if (enemy.getLocation().distance(center) <= radius) {
                combatService.applyAbilityDamage(player, enemy, damage, getName());

                debuffManager.tryApply(enemy, DebuffType.SILENCE, silenceDurationMs);

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
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Silence Duration", (silenceDurationMs / 1000) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (chargeTimeMillis / 1000) + "s")
        );
    }
}