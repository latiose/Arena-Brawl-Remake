package org.latios.arenaBrawl.abilities;

public enum AbilitySlot {
    OFFENSIVE("§c"),
    UTILITY("§e"),
    SUPPORT("§a"),
    ULTIMATE("§6");

    private final String chatColor;

    AbilitySlot(String chatColor) {
        this.chatColor = chatColor;
    }

    /** Chat color used for this category in match-start messages. */
    public String getChatColor() {
        return chatColor;
    }
}
