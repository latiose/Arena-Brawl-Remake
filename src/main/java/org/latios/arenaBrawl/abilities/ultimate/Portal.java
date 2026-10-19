package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.abilities.structures.PortalStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;


public class Portal implements Ability {


    private final long chargeTimeMillis;
    private final double maxTargetDistance;
    private final double portalRadius;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final StructureManager structureManager;
    private final AbilityConfig config;

    public Portal(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                  TeamManager teamManager, StructureManager structureManager, AbilityConfig config) {
        this.plugin = plugin;
        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.maxTargetDistance = config.getDouble("max-target-distance", 20.0);
        this.portalRadius = config.getDouble("portal-radius", 1.4);

        this.cost = new UltimateCost(cooldownManager, usageManager, "portal");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.structureManager = structureManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Portal";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "portal", chargeTimeMillis);
    }

    @Override
    public String getDescription() {
        return "Aim at the top of a ground block to open a permanent two-way portal between that block "
                + "and your current position. It lasts until the match ends.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", (int) maxTargetDistance + "m"),
                new AbilityStat("Duration", "Permanent"),
                new AbilityStat("Target", "Ground block"),
                new AbilityStat("Uses", "1 per match")
        );
    }

    @Override
    public boolean activate(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxTargetDistance,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        BlockFace face = result.getHitBlockFace();
        if (face == null || face.getModY() <= 0) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Block ground = result.getHitBlock();
        Block standingSpace = ground.getRelative(BlockFace.UP);

        if (!standingSpace.isPassable() || !standingSpace.getRelative(BlockFace.UP).isPassable()) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        Location portalB = standingSpace.getLocation().add(0.5, 0.0, 0.5);
        Location portalA = player.getLocation().clone();

        if (portalA.getWorld() == null || !portalA.getWorld().equals(portalB.getWorld())
                || portalA.distanceSquared(portalB) < 9.0) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        PortalStructure portal = new PortalStructure(player, portalA, portalB, plugin, teamManager, portalRadius);
        structureManager.register(portal);
        portal.start();

        MatchSoundUtils.play(config, player, Sound.BLOCK_PORTAL_TRIGGER, 1.2f, 1.4f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        portalA.getWorld().spawnParticle(Particle.PORTAL, portalA.clone().add(0, 1.0, 0), 40, 0.4, 0.8, 0.4, 0.5);
        portalB.getWorld().spawnParticle(Particle.PORTAL, portalB.clone().add(0, 1.0, 0), 40, 0.4, 0.8, 0.4, 0.5);

        player.sendMessage("§5Portal§3 opened! Walk into either end to travel.");

        return true;
    }
}
