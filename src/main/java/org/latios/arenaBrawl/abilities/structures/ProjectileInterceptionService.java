package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central registry used by every projectile implementation to ask the active
 * {@link AegisScreenStructure}s whether their path is blocked.
 * <p>
 * Real Bukkit projectiles are handled by the screen's own tick task, but many
 * abilities in this plugin move custom entities (armor stands, item displays,
 * living entities) or are pure particle beams. Those implementations call this
 * service once per tick so they can be intercepted without knowing anything
 * about the screen class.
 */
public final class ProjectileInterceptionService {

    private static final List<AegisScreenStructure> SCREENS = new ArrayList<>();

    /**
     * Projectiles removed by a screen this tick. Custom projectile tasks check
     * this before treating a vanished projectile as an impact, otherwise a
     * snowball removed by the screen would still explode on the screen's side.
     */
    private static final Map<UUID, Long> INTERCEPTED = new HashMap<>();
    private static final long INTERCEPT_MEMORY_MS = 2_000L;

    private ProjectileInterceptionService() {
    }

    public static void register(AegisScreenStructure screen) {
        if (screen != null && !SCREENS.contains(screen)) {
            SCREENS.add(screen);
        }
    }

    public static void unregister(AegisScreenStructure screen) {
        SCREENS.remove(screen);
    }

    /** Records that a screen destroyed this projectile so its task can stay silent. */
    public static void markIntercepted(UUID projectileId) {
        if (projectileId == null) return;

        long now = System.currentTimeMillis();
        INTERCEPTED.entrySet().removeIf(entry -> now - entry.getValue() > INTERCEPT_MEMORY_MS);
        INTERCEPTED.put(projectileId, now);
    }

    /** @return {@code true} when the projectile was destroyed by an Aegis screen. */
    public static boolean wasIntercepted(UUID projectileId) {
        if (projectileId == null) return false;

        Long at = INTERCEPTED.get(projectileId);
        if (at == null) return false;

        if (System.currentTimeMillis() - at > INTERCEPT_MEMORY_MS) {
            INTERCEPTED.remove(projectileId);
            return false;
        }
        return true;
    }

    /**
     * Tests the whole segment travelled by a projectile during one tick.
     *
     * @return {@code true} when an enemy screen intercepted the projectile; the
     * caller must then remove its own projectile.
     */
    public static boolean tryIntercept(Player shooter, Location from, Location to) {
        if (shooter == null || from == null || to == null) {
            return false;
        }

        for (AegisScreenStructure screen : snapshot()) {
            if (screen.tryIntercept(shooter, from, to)) {
                return true;
            }
        }
        return false;
    }

    /** Point test used by hitscan and instant beam abilities. */
    public static boolean isBlocked(Player shooter, Location point) {
        if (shooter == null || point == null) {
            return false;
        }

        for (AegisScreenStructure screen : snapshot()) {
            if (screen.blocksPoint(shooter, point)) {
                return true;
            }
        }
        return false;
    }

    private static List<AegisScreenStructure> snapshot() {
        return new ArrayList<>(SCREENS);
    }
}
