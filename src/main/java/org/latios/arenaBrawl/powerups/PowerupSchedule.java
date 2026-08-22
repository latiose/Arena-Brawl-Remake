
package org.latios.arenaBrawl.powerups;

public record PowerupSchedule(long cycleMillis, long windowStartMillis, long windowEndMillis) {
    public boolean isWithinWindow(long cyclePosition) {
        return cyclePosition >= windowStartMillis && cyclePosition < windowEndMillis;
    }
}