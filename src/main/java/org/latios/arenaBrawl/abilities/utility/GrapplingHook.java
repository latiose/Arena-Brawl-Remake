package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class GrapplingHook implements Ability {

    private static final String COOLDOWN_KEY = "grapplinghook";

    private final AbilityCost cost;
    private final AbilityConfig config;
    private final long cooldownMs;
    private final double maxDistance;
    private final double maxWallHeight;
    private final double extraHeight;
    private final double pullSpeed;
    private final double climbSpeed;
    private final double ledgePush;

    public GrapplingHook(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                         AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.maxDistance = config.getDouble("max-target-distance", 25.0);
        this.maxWallHeight = config.getDouble("max-wall-height", 14.0);
        this.extraHeight = config.getDouble("extra-height", 3.0);
        this.pullSpeed = config.getDouble("pull-speed", 1.1);
        this.climbSpeed = config.getDouble("climb-speed", 0.85);
        this.ledgePush = config.getDouble("ledge-push", 0.45);
        this.cost = new CooldownCost(cooldownManager, COOLDOWN_KEY, cooldownMs, upgradeManager);
        this.config = config;
    }

    @Override
    public String getName() { return "Grappling Hook"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires a grappling hook at a wall, pulls you to it and launches you "
                + (int) extraHeight + " blocks above its top.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s"),
                new AbilityStat("Range", (int) maxDistance + " blocks"),
                new AbilityStat("Max Wall Height", (int) maxWallHeight + " blocks"),
                new AbilityStat("Extra Height", (int) extraHeight + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                maxDistance,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null) {
            player.sendMessage("§cYou must target a block within range.");
            return false;
        }

        Block hitBlock = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();

        // Find the top of the wall: first non-solid block above the hit block
        Block top = hitBlock;
        int scanned = 0;
        while (!top.isPassable() && scanned <= maxWallHeight) {
            top = top.getRelative(BlockFace.UP);
            scanned++;
        }
        if (scanned > maxWallHeight) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }
        if (top.getY() - player.getLocation().getY() > maxWallHeight) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        // The ledge needs room for the player (2 blocks of air)
        if (!top.isPassable() || !top.getRelative(BlockFace.UP).isPassable()) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        final double topY = top.getY();

        // Horizontal direction pointing INTO the wall
        final Vector intoWall;
        if (face.getModY() != 0) {
            intoWall = player.getLocation().getDirection().setY(0).normalize();
        } else {
            intoWall = new Vector(-face.getModX(), 0, -face.getModZ()).normalize();
        }

        final Location hitPos = result.getHitPosition().toLocation(player.getWorld());
        // Point just in front of the wall where the player gets pulled to
        final Location climbPoint = hitPos.clone().add(face.getDirection().multiply(0.6));

        MatchSoundUtils.play(config, player, Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 0.8f);
        hitPos.getWorld().playSound(hitPos, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.0f);
        hitPos.getWorld().spawnParticle(Particle.CRIT, hitPos, 8, 0.1, 0.1, 0.1, 0.2);

        new BukkitRunnable() {
            int ticks = 0;
            boolean climbing = false;

            @Override
            public void run() {
                ticks++;

                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR || ticks > 60) {
                    cancel();
                    return;
                }

                player.setFallDistance(0);
                drawRope(player, hitPos);

                Location body = player.getLocation().add(0, 1.0, 0);

                if (!climbing) {
                    // Phase 1: pull towards the wall
                    Vector toTarget = climbPoint.toVector().subtract(body.toVector());
                    if (toTarget.length() < 1.5) {
                        climbing = true;
                    } else {
                        player.setVelocity(toTarget.normalize().multiply(pullSpeed));
                        return;
                    }
                }

                // Phase 2: climb up the wall
                double y = player.getLocation().getY();
                if (y < topY + extraHeight) {
                    Vector v = new Vector(0, climbSpeed, 0);
                    // Once above the ledge, start moving over it
                    if (y >= topY + 0.5) {
                        v.add(intoWall.clone().multiply(ledgePush));
                    }
                    player.setVelocity(v);
                    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 2, 0.2, 0.1, 0.2, 0.01);
                    return;
                }

                // Finish: final push over the ledge
                player.setVelocity(intoWall.clone().multiply(ledgePush).setY(0.25));
                player.setFallDistance(0);
                MatchSoundUtils.play(config, player, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.4f);
                cancel();
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    /** Draws the rope from the player's hand to the anchor point. */
    private void drawRope(Player player, Location anchor) {
        Location from = player.getEyeLocation().add(0, -0.4, 0);
        Vector line = anchor.toVector().subtract(from.toVector());
        double length = line.length();
        if (length < 0.1) return;
        Vector step = line.clone().normalize().multiply(0.5);

        Location p = from.clone();
        for (double d = 0; d < length; d += 0.5) {
            p.getWorld().spawnParticle(Particle.DUST, p, 1, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(150, 110, 70), 0.9f));
            p.add(step);
        }
    }
}