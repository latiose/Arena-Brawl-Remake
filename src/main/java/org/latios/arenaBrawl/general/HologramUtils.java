package org.latios.arenaBrawl.general;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public final class HologramUtils {

    private static final double DEFAULT_MIN_RADIUS = 0.75;
    private static final double DEFAULT_MAX_RADIUS = 1.25;
    private static final double DEFAULT_MIN_HEIGHT = 0.9;
    private static final double DEFAULT_MAX_HEIGHT = 1.9;

    private HologramUtils() {
    }

    public static Location randomAround(Player player) {
        return randomAround(player, DEFAULT_MIN_RADIUS, DEFAULT_MAX_RADIUS,
                DEFAULT_MIN_HEIGHT, DEFAULT_MAX_HEIGHT);
    }

    public static Location randomAround(Player player, double minRadius, double maxRadius,
                                        double minHeight, double maxHeight) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0, Math.PI * 2);
        double radius = random.nextDouble(minRadius, maxRadius);

        return player.getLocation().clone().add(
                Math.cos(angle) * radius,
                random.nextDouble(minHeight, maxHeight),
                Math.sin(angle) * radius
        );
    }
}
