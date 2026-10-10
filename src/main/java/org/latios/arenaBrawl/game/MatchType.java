package org.latios.arenaBrawl.game;

public enum MatchType {
    DUEL("1v1", 2, 1),
    TEAMS("2v2", 2, 2),
    FFA("1v1v1v1", 4, 1);

    private final String displayName;
    private final int teamCount, teamSize;
    MatchType(String displayName, int teamCount, int teamSize) {
        this.displayName = displayName;
        this.teamCount = teamCount;
        this.teamSize = teamSize;
    }

    public String getDisplayName() { return displayName; }
    public int getTeamCount() { return teamCount; }
    public int getTeamSize() { return teamSize; }
    public int playersNeeded() { return teamCount * teamSize; }
}