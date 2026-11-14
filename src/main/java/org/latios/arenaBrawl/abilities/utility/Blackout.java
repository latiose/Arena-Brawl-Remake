package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Blackout implements Ability {

    private final long cooldownMs;
    private final double radius;
    private final long blindnessDurationMs;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public Blackout(CooldownManager cooldownManager, TeamManager teamManager, DebuffManager debuffManager,
                    CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.radius = config.getDouble("radius", 12.0);
        this.blindnessDurationMs = config.getLong("blindness-duration-ms", 3000L);

        this.cost = new CooldownCost(cooldownManager, "blackout", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Blackout";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Plunges every opponent within " + (int) radius + " blocks into darkness, blinding them for "
                + (blindnessDurationMs / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Blindness", (blindnessDurationMs / 1000.0) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation().clone();

        player.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_SCREAM, 8.0f, 0.6f);
        player.getWorld().playSound(center, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 8.0f, 0.8f);

        spawnRings(center);

        for (Player enemy : center.getWorld().getPlayers()) {
            if (enemy.equals(player) || enemy.isDead()) continue;
            if (enemy.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, enemy)) continue;
            if (enemy.getLocation().distanceSquared(center) > radius * radius) continue;

            debuffManager.tryApply(player, enemy, DebuffType.BLINDNESS, blindnessDurationMs);

            enemy.getWorld().spawnParticle(Particle.SQUID_INK, enemy.getLocation().add(0, 1.5, 0),
                    20, 0.35, 0.45, 0.35, 0.02);
        }


        return true;
    }

    private void spawnRings(Location center) {
        int rings = 4;
        int points = 32;

        for (int ring = 0; ring < rings; ring++) {
            double layerRadius = radius * (ring + 1) / rings;
            double y = 0.2 + ring * 0.4;

            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double x = Math.cos(angle) * layerRadius;
                double z = Math.sin(angle) * layerRadius;

                Location point = center.clone().add(x, y, z);
                center.getWorld().spawnParticle(Particle.SQUID_INK, point, 1, 0, 0, 0, 0.01);
                center.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0, 0, 0, 0.01);
            }
        }
    }
}
