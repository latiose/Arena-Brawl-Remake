
package org.latios.arenaBrawl.debuffs;

public enum DebuffType {
    IMMOBILIZE("Immobilize", true, "#00FF00"),
    STUN("Stun", true, "#FFFFFF"),
    POLYMORPH("Polymorph", true, "#FFFF00"),
    SLOW("Slow", false, "#A020F0"),
    POISON("Poison", false, "#00FF00"),
    ANTIHEAL("AntiHeal", false, "#FF0000"),
    SILENCE("Silence", false, "#00008B");
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