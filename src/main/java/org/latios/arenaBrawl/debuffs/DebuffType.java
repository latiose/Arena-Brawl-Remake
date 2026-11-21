
package org.latios.arenaBrawl.debuffs;

public enum DebuffType {
    IMMOBILIZE("Immobilize", true, "#00FF00"),
    STUN("Stun", true, "#FFFFFF"),
    SLEEP("Sleep", true, "#8FA8FF"),
    POLYMORPH("Polymorph", true, "#FFFF00"),
    SLOW("Slow", false, "#A020F0"),
    POISON("Poison", false, "#00FF00"),
    ANTIHEAL("AntiHeal", false, "#FF0000"),
    SILENCE("Silence", false, "#00008B"),
    BLINDNESS("Blindness", false, "#404040");
    private final String displayName;

    private final boolean immobilizing;
    private final String color;
    DebuffType(String displayName, boolean immobilizing, String color) {
        this.displayName = displayName;
        this.immobilizing = immobilizing;
        this.color = color;
    }



    public String getDisplayName() {
        return displayName;
    }

    public String getColor() {
        return this.color;
    }
    public boolean isImmobilizing() { return immobilizing; }
}