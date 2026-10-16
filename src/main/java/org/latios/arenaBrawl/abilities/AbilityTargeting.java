package org.latios.arenaBrawl.abilities;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.Block;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.function.Predicate;

public class AbilityTargeting {

    private static final double STEP_SIZE = 0.2;

    public static Player findEnemyAlongRay(Player caster, TeamManager teamManager, double maxRange) {
        return findAlongRay(caster, maxRange, c -> isValidEnemyTarget(caster, c, teamManager));
    }

    public static Player findAllyAlongRay(Player caster, TeamManager teamManager, double maxRange) {
        return findAlongRay(caster, maxRange, c -> isValidAllyTarget(caster, c, teamManager));
    }

    public static Player findTargetAlongRay(Player caster, double maxRange) {
        return findAlongRay(caster, maxRange, c -> isValidAnyTarget(caster, c));
    }

    /**
     * Casts a ray from the caster's eyes. If multiple valid targets lie along the ray,
     * the one at the END of the line (furthest along the ray) is returned.
     * Targets do not block the ray; only full solid blocks do.
     */
    private static Player findAlongRay(Player caster, double maxRange, Predicate<Player> validator) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Vector behindStep = direction.clone().multiply(-0.3 * STEP_SIZE);
        Location behindCursor = eye.clone();

        for (double traveled = 0; traveled <= 1.0; traveled += STEP_SIZE) {
            behindCursor.add(behindStep);

            if (blocksRay(behindCursor.getBlock())) break;

            for (Player candidate : behindCursor.getWorld().getPlayers()) {
                if (!validator.test(candidate)) continue;

                if (isInsideHitbox(candidate, behindCursor)) {
                    return candidate;
                }
            }
        }

        Location cursor = eye.clone();
        Vector step = direction.clone().multiply(STEP_SIZE);
        Player furthestTarget = null;

        for (double traveled = 0; traveled < maxRange; traveled += STEP_SIZE) {
            cursor.add(step);

            if (blocksRay(cursor.getBlock())) {
                break;
            }

            Player stepBest = null;
            double stepBestAngle = Double.MAX_VALUE;

            for (Player candidate : cursor.getWorld().getPlayers()) {
                if (!validator.test(candidate)) continue;
                if (!isInsideHitbox(candidate, cursor)) continue;

                Vector toCandidate = candidate.getEyeLocation().toVector().subtract(eye.toVector());
                if (toCandidate.lengthSquared() < 1e-9) continue;

                double angle = direction.angle(toCandidate.normalize());
                if (angle < stepBestAngle) {
                    stepBestAngle = angle;
                    stepBest = candidate;
                }
            }

            if (stepBest != null) {
                furthestTarget = stepBest;
            }
        }

        return furthestTarget;
    }

    private static boolean isValidEnemyTarget(Player caster, Player candidate, TeamManager teamManager) {
        if (!isValidAnyTarget(caster, candidate)) return false;
        return teamManager.isEnemy(caster, candidate);
    }

    private static boolean isValidAllyTarget(Player caster, Player candidate, TeamManager teamManager) {
        if (!isValidAnyTarget(caster, candidate)) return false;
        return teamManager.isAlly(caster, candidate);
    }

    private static boolean isValidAnyTarget(Player caster, Player candidate) {
        if (candidate.equals(caster)) return false;
        if (candidate.getGameMode() == GameMode.SPECTATOR) return false;
        return true;
    }

    private static boolean isInsideHitbox(Player target, Location point) {
        BoundingBox box = target.getBoundingBox().expand(0.4, 0.4, 0.4);
        return box.contains(point.getX(), point.getY(), point.getZ());
    }

    private static boolean blocksRay(Block block) {
        if (!block.getType().isSolid()) return false;
        BoundingBox box = block.getBoundingBox();
        return box.getWidthX() >= 0.99 && box.getHeight() >= 0.99 && box.getWidthZ() >= 0.99;
    }

    public static Location findBehindLocation(Player target) {
        Location targetLoc = target.getLocation();
        Vector facing = targetLoc.getDirection().clone();
        facing.setY(0);
        if (facing.lengthSquared() < 1e-6) facing = new Vector(0, 0, 1);
        facing.normalize();

        Vector behindDirection = facing.clone().multiply(-1);
        Location base = targetLoc.clone();

        Location result = base.clone();
        double step = 0.1;

        for (double traveled = step; traveled <= 1.0; traveled += step) {
            Location candidate = base.clone().add(behindDirection.clone().multiply(traveled));
            if (blocksRay(candidate.getBlock())) {
                break;
            }
            result = candidate;
        }

        result.setDirection(facing);
        return result;
    }
}