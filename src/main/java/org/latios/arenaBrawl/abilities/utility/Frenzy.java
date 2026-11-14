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
import org.latios.arenaBrawl.general.SpeedBuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class Frenzy implements Ability {

    private final long cooldownMs;
    private final int speedAmplifier;
    private final long speedDurationMs;
    private final double radius;
    private final long slowDurationMs;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final SpeedBuffManager speedBuffManager;
    private final AbilityConfig config;

    public Frenzy(CooldownManager cooldownManager, TeamManager teamManager, DebuffManager debuffManager,
                  SpeedBuffManager speedBuffManager, CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.speedAmplifier = config.getInt("speed-amplifier", 1);
        this.speedDurationMs = config.getLong("speed-duration-ms", 3000L);
        this.radius = config.getDouble("radius", 5.0);
        this.slowDurationMs = config.getLong("slow-duration-ms", 3000L);

        this.cost = new CooldownCost(cooldownManager, "frenzy", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.speedBuffManager = speedBuffManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Frenzy";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Grants you Speed II for " + (speedDurationMs / 1000.0)
                + "s and slows every opponent within " + (int) radius + " blocks for "
                + (slowDurationMs / 1000.0) + "s.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Speed", "Speed II for " + (speedDurationMs / 1000.0) + "s"),
                new AbilityStat("Slow", (slowDurationMs / 1000.0) + "s"),
                new AbilityStat("Radius", (int) radius + "m"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location center = player.getLocation().clone();

        speedBuffManager.applyBuff(player, speedAmplifier, speedDurationMs, "frenzy");

        player.getWorld().playSound(center, Sound.ENTITY_RAVAGER_ROAR, 8.0f, 1.3f);
        player.getWorld().playSound(center, Sound.ENTITY_HORSE_ANGRY, 8.0f, 1.6f);

        spawnRing(center);

        for (Player enemy : center.getWorld().getPlayers()) {
            if (enemy.equals(player) || enemy.isDead()) continue;
            if (enemy.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, enemy)) continue;
            if (enemy.getLocation().distanceSquared(center) > radius * radius) continue;

            debuffManager.tryApply(player, enemy, DebuffType.SLOW, slowDurationMs);

            enemy.getWorld().spawnParticle(Particle.WITCH, enemy.getLocation().add(0, 1.0, 0),
                    12, 0.3, 0.5, 0.3, 0.02);
        }


        return true;
    }

    private void spawnRing(Location center) {
        int points = 40;

        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI / points) * i;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            Location point = center.clone().add(x, 0.2, z);
            center.getWorld().spawnParticle(Particle.WITCH, point, 2, 0, 0, 0, 0.02);
            center.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0.02);
        }
    }
}
