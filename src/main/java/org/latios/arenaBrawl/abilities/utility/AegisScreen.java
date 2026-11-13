package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.AegisScreenStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

/**
 * Projects a particle-only screen in front of the caster that destroys enemy projectiles crossing
 * it. Allies can shoot through it, and it does not block movement.
 */
public class AegisScreen implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final int hitsToDestroy;
    private final double distance;
    private final double width;
    private final double height;

    private final AbilityCost cost;
    private final StructureManager structureManager;
    private final TeamManager teamManager;

    public AegisScreen(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                       StructureManager structureManager, TeamManager teamManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 35000L);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.hitsToDestroy = config.getInt("hits-to-destroy", 5);
        this.distance = config.getDouble("distance", 2.0);
        this.width = config.getDouble("width", 3.0);
        this.height = config.getDouble("height", 3.0);

        this.cost = new CooldownCost(cooldownManager, "aegisscreen", cooldownMs, upgradeManager);
        this.structureManager = structureManager;
        this.teamManager = teamManager;
    }

    @Override
    public String getName() {
        return "Aegis Screen";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Projects a 3x3 shimmering screen that destroys enemy projectiles crossing it. "
                + "Allies can shoot through it and it does not block movement.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Size", "3x3 blocks"),
                new AbilityStat("Melee hits to destroy", String.valueOf(hitsToDestroy)),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getEyeLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1.0E-6) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }
        direction.normalize();

        Location center = player.getLocation().clone().add(direction.clone().multiply(distance));
        center.add(0, 1.5, 0);

        AegisScreenStructure screen = new AegisScreenStructure(player, center, direction, width, height,
                plugin, teamManager, durationMs, hitsToDestroy);
        structureManager.register(screen);
        screen.start();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
        player.getWorld().spawnParticle(Particle.END_ROD, center, 30, width / 2.0, height / 2.0, width / 2.0, 0.02);

        return true;
    }
}
