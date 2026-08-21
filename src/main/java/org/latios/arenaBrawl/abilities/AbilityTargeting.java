// abilities/AbilityTargeting.java
package org.latios.arenaBrawl.abilities;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

public class AbilityTargeting {

    private static final double STEP_SIZE = 0.15;

    /**
     * Marches a ray from the caster's eyes up to maxRange, ignoring non-full blocks
     * (slabs, stairs, fences, etc.) but stopping at full solid cubes. At each step it
     * checks whether the point falls inside any enemy's synthetic hitbox (see
     * isInsideSyntheticHitbox), returning the first enemy found.
     *
     * NOTE: the synthetic hitbox formula below is a best-effort implementation of a
     * pseudo-mathematical description with no verified source, since the referenced
     * Desmos graph could not be accessed. Treat the constants as a starting point.
     */
    public static Player findEnemyAlongRay(Player caster, TeamManager teamManager, double maxRange) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location cursor = eye.clone();

        for (double traveled = 0; traveled < maxRange; traveled += STEP_SIZE) {
            cursor.add(direction.clone().multiply(STEP_SIZE));

            Block block = cursor.getBlock();
            if (blocksRay(block)) {
                return null; // ray stopped by a full solid wall
            }

            for (Player candidate : cursor.getWorld().getPlayers()) {
                if (candidate.equals(caster)) continue;
                if (!teamManager.isEnemy(caster, candidate)) continue;

                if (isInsideSyntheticHitbox(candidate, cursor)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    /**
     * Treats a block as a wall only if it occupies a full 1x1x1 cube (stone, dirt, full
     * wood blocks, etc.). Partial blocks (slabs, stairs, fences, carpets) let the ray
     * pass through, matching "travels through transparent blocks".
     */
    private static boolean blocksRay(Block block) {
        if (!block.getType().isSolid()) return false;

        BoundingBox box = block.getBoundingBox();
        return box.getWidthX() >= 0.99 && box.getHeight() >= 0.99 && box.getWidthZ() >= 0.99;
    }

    /**
     * Synthetic hitbox per the given formula:
     * - vertical: round(y) - 2  to  round(y) + 2
     * - horizontal (x and z independently):
     *     base = floor(abs(coord))
     *     extends from (base - 1) to (base + 1), in absolute-value space,
     *     with the upper bound pushed out by 1 extra unit if the fractional
     *     part of abs(coord) exceeds 0.25, then mirrored back to the coord's sign.
     */
    private static boolean isInsideSyntheticHitbox(Player target, Location point) {
        Location loc = target.getLocation();

        double minY = Math.round(loc.getY()) - 2;
        double maxY = Math.round(loc.getY()) + 2;
        if (point.getY() < minY || point.getY() > maxY) return false;

        if (!isWithinAxis(loc.getX(), point.getX())) return false;
        if (!isWithinAxis(loc.getZ(), point.getZ())) return false;

        return true;
    }

    private static boolean isWithinAxis(double targetCoord, double pointCoord) {
        double absCoord = Math.abs(targetCoord);
        double base = Math.floor(absCoord);
        double frac = absCoord - base;
        double extra = frac > 0.25 ? 1.0 : 0.0;

        double lowerAbs = base - 1;
        double upperAbs = base + 1 + extra;

        double min, max;
        if (targetCoord >= 0) {
            min = lowerAbs;
            max = upperAbs;
        } else {
            min = -upperAbs;
            max = -lowerAbs;
        }

        return pointCoord >= min && pointCoord <= max;
    }

    /**
     * Finds a safe spot up to 1 block behind the target's facing direction, on the
     * target's current Y level, stopping short of any full solid wall.
     */
    public static Location findBehindLocation(Player target) {
        Location targetLoc = target.getLocation();
        Vector facing = targetLoc.getDirection().clone();
        facing.setY(0);
        if (facing.lengthSquared() < 1e-6) facing = new Vector(0, 0, 1);
        facing.normalize();

        Vector behindDirection = facing.clone().multiply(-1);
        Location base = targetLoc.clone(); // same Y level as the target

        Location result = base.clone();
        double step = 0.1;

        for (double traveled = step; traveled <= 1.0; traveled += step) {
            Location candidate = base.clone().add(behindDirection.clone().multiply(traveled));
            if (blocksRay(candidate.getBlock())) {
                break; // stop before entering a wall, keep last safe `result`
            }
            result = candidate;
        }

        result.setDirection(facing); // face back toward the target
        return result;
    }
}