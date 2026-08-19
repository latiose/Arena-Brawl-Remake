
package org.latios.arenaBrawl.debuffs;

public enum DebuffType {
    IMMOBILIZE("Immobilize"),
    STUN("Stun"),
    POLYMORPH("Polymorph");

    private final String displayName;

    DebuffType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}