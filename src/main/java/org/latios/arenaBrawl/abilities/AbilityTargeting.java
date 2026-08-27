package org.latios.arenaBrawl.abilities;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.team.TeamManager;

public class AbilityTargeting {

    private static final double STEP_SIZE = 0.2;

    public static Player findEnemyAlongRay(Player caster, TeamManager teamManager, double maxRange) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Vector behindDirection = direction.clone().multiply(-1);
        Location behindCursor = eye.clone();

        for (double traveled = 0; traveled <= 1.0; traveled += STEP_SIZE) {
            behindCursor.add(behindDirection.clone().multiply(STEP_SIZE));

            if (blocksRay(behindCursor.getBlock())) break;

            for (Player candidate : behindCursor.getWorld().getPlayers()) {
                if (!isValidEnemyTarget(caster, candidate, teamManager)) continue;

                if (isInsideHitbox(candidate, behindCursor)) {
                    return candidate;
                }
            }
        }

        Location cursor = eye.clone();
        Player bestTarget = null;
        double bestAngle = Double.MAX_VALUE;

        for (double traveled = 0; traveled < maxRange; traveled += STEP_SIZE) {
            cursor.add(direction.clone().multiply(STEP_SIZE));

            Block block = cursor.getBlock();
            if (blocksRay(block)) {
                break;
            }

            for (Player candidate : cursor.getWorld().getPlayers()) {
                if (!isValidEnemyTarget(caster, candidate, teamManager)) continue;

                if (isInsideHitbox(candidate, cursor)) {
                    Vector toCandidate = candidate.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                    double angle = direction.angle(toCandidate);

                    if (angle < bestAngle) {
                        bestAngle = angle;
                        bestTarget = candidate;
                    }
                }
            }

            if (bestTarget != null) {
                return bestTarget;
            }
        }

        return null;
    }

    public static Player findTargetAlongRay(Player caster, double maxRange) {
        Location eye = caster.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        Vector behindDirection = direction.clone().multiply(-1);
        Location behindCursor = eye.clone();

        for (double traveled = 0; traveled <= 1.0; traveled += STEP_SIZE) {
            behindCursor.add(behindDirection.clone().multiply(STEP_SIZE));

            if (blocksRay(behindCursor.getBlock())) break;

            for (Player candidate : behindCursor.getWorld().getPlayers()) {
                if (!isValidAnyTarget(caster, candidate)) continue;

                if (isInsideHitbox(candidate, behindCursor)) {
                    return candidate;
                }
            }
        }

        Location cursor = eye.clone();
        Player bestTarget = null;
        double bestAngle = Double.MAX_VALUE;

        for (double traveled = 0; traveled < maxRange; traveled += STEP_SIZE) {
            cursor.add(direction.clone().multiply(STEP_SIZE));

            Block block = cursor.getBlock();
            if (blocksRay(block)) {
                break;
            }

            for (Player candidate : cursor.getWorld().getPlayers()) {
                if (!isValidAnyTarget(caster, candidate)) continue;

                if (isInsideHitbox(candidate, cursor)) {
                    Vector toCandidate = candidate.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                    double angle = direction.angle(toCandidate);

                    if (angle < bestAngle) {
                        bestAngle = angle;
                        bestTarget = candidate;
                    }
                }
            }

            if (bestTarget != null) {
                return bestTarget;
            }
        }

        return null;
    }

    private static boolean isValidEnemyTarget(Player caster, Player candidate, TeamManager teamManager) {
        if (!isValidAnyTarget(caster, candidate)) return false;
        return teamManager.isEnemy(caster, candidate);
    }

    private static boolean isValidAnyTarget(Player caster, Player candidate) {
        if (candidate.equals(caster)) return false;
        if (candidate.getGameMode() == GameMode.SPECTATOR) return false;
        return true;
    }

    private static boolean isInsideHitbox(Player target, Location point) {
        BoundingBox box = target.getBoundingBox().expand(0.3, 0.3, 0.3);
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