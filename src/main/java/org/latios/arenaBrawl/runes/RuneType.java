
package org.latios.arenaBrawl.runes;

public enum RuneType {

    SPEED("Rune of Speed", 0.25),
    SLOW("Rune of Slow", 0.15),
    DAMAGE("Rune of Damage", 0.50),
    ENERGY("Rune of Energy", 0.15);

    private final String displayName;
    private final double procChance;

    RuneType(String displayName, double procChance) {
        this.displayName = displayName;
        this.procChance = procChance;
    }

    public String getDisplayName() { return displayName; }
    public double getProcChance() { return procChance; }
}