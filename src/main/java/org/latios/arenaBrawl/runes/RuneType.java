package org.latios.arenaBrawl.runes;

public enum RuneType {
    SPEED("Rune of Speed", "§f"),
    SLOW("Rune of Slow", "§5"),
    DAMAGE("Rune of Damage", "§c"),
    ENERGY("Rune of Energy", "§e"),
    CONFUSION("Rune of Confusion", "§0"),
    IMMOBILIZE("Rune of Immobilization", "§a"),
    HEALING("Rune of Healing", "§a"),
    ONESHOT("Rune of One Shot", "§c"),
    THORNS("Rune of Thorns", "§4"),
    WEAKNESS("Rune of Weakness", "§6"),
    HUNGER("Rune of Hunger", "§6"),
    STARVATION("Rune of Starvation", "§8");


  private final String displayName;
  /** Chat color used when the rune activates, also used for match-start announcements. */
  private final String chatColor;

  RuneType(String displayName, String chatColor) {
      this.displayName = displayName;
      this.chatColor = chatColor;
  }


  public String getDisplayName() { return displayName; }

  /** Chat color used when this rune activates ("Your ... was activated!"). */
  public String getChatColor() { return chatColor; }

    public String getConfigId() {
        return name().toLowerCase();
    }
}
