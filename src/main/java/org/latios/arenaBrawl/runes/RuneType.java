package org.latios.arenaBrawl.runes;

public enum RuneType {
    SPEED("Rune of Speed"),
    SLOW("Rune of Slow"),
    DAMAGE("Rune of Damage"),
    ENERGY("Rune of Energy");

    /*
  SPEED("Rune of Speed", 0.25),
  SLOW("Rune of Slow", 0.15),
  DAMAGE("Rune of Damage", 0.50),
  ENERGY("Rune of Energy", 0.15);
  */

  private final String displayName;

  RuneType(String displayName) {
      this.displayName = displayName;
  }


  public String getDisplayName() { return displayName; }

    public String getConfigId() {
        return name().toLowerCase();
    }
}