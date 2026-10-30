package org.latios.arenaBrawl.runes;

public enum RuneType {
    SPEED("Rune of Speed"),
    SLOW("Rune of Slow"),
    DAMAGE("Rune of Damage"),
    ENERGY("Rune of Energy"),
    CONFUSION("Rune of Confusion"),
    IMMOBILIZE("Rune of Immobilization");


  private final String displayName;

  RuneType(String displayName) {
      this.displayName = displayName;
  }


  public String getDisplayName() { return displayName; }

    public String getConfigId() {
        return name().toLowerCase();
    }
}