
package org.latios.arenaBrawl.debuffs;

public enum DebuffType {
    IMMOBILIZE("Immobilize",true),
    STUN("Stun", true),
    POLYMORPH("Polymorph", true),
    SLOW("Slow", false),
    POISON("Poison", false),
    ANTIHEAL("AntiHeal", false),
    SILENCE("Silence", false);

    private final String displayName;

    private final boolean immobilizing;
    DebuffType(String displayName, boolean immobilizing) {
        this.displayName = displayName;
        this.immobilizing = immobilizing;
    }



    public String getDisplayName() {
        return displayName;
    }

    public boolean isImmobilizing() { return immobilizing; }
}