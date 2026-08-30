package org.latios.arenaBrawl.lobby;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.latios.arenaBrawl.abilities.AbilityPersistenceManager;
import org.latios.arenaBrawl.abilities.AbilityRegistry;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.rating.LeaderboardEntry;
import org.latios.arenaBrawl.rating.RatingManager;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardSignManager {

    private static final int TOP_SIZE = 20;

    private final RatingManager ratingManager;
    private final AbilityPersistenceManager abilityPersistenceManager;
    private final AbilityRegistry abilityRegistry;

    private List<Location> signLocations = List.of(); // ordered rank 1 -> N
    private List<CachedEntry> cachedEntries = List.of();
    private int rotationIndex = 0;

    private record CachedEntry(LeaderboardEntry entry, List<String> abilityNames) {}

    public LeaderboardSignManager(RatingManager ratingManager, AbilityPersistenceManager abilityPersistenceManager,
                                  AbilityRegistry abilityRegistry) {
        this.ratingManager = ratingManager;
        this.abilityPersistenceManager = abilityPersistenceManager;
        this.abilityRegistry = abilityRegistry;
    }

    public void configureSignLocations(List<Location> locations) {
        this.signLocations = locations;
    }

    /** Recomputes the top 20 and each player's 4 equipped ability names. Call every 5 minutes. */
    public void refresh() {
        List<LeaderboardEntry> top = ratingManager.getTopRatingsDetailed(TOP_SIZE);
        List<CachedEntry> newCache = new ArrayList<>();

        for (LeaderboardEntry entry : top) {
            List<String> abilityNames = new ArrayList<>();

            for (AbilitySlot slot : AbilitySlot.values()) {
                String abilityId = abilityPersistenceManager.load(entry.uuid(), slot);
                if (abilityId == null) {
                    abilityId = abilityRegistry.getDefault(slot);
                }
                var preview = abilityRegistry.createPreview(slot, abilityId);
                abilityNames.add(preview.getName());
            }

            newCache.add(new CachedEntry(entry, abilityNames));
        }

        this.cachedEntries = newCache;
        rotationIndex = 0;
        renderAllSigns();
    }

    /** Rotates which equipped ability is shown on the bottom line. Call every ~2 seconds. */
    public void rotate() {
        rotationIndex = (rotationIndex + 1) % AbilitySlot.values().length;
        renderAllSigns();
    }

    private void renderAllSigns() {
        for (int i = 0; i < signLocations.size(); i++) {
            Location loc = signLocations.get(i);

            if (i < cachedEntries.size()) {
                writeSign(loc, i + 1, cachedEntries.get(i));
            } else {
                clearSign(loc);
            }
        }
    }

    private void writeSign(Location loc, int position, CachedEntry cached) {
        if (loc.getWorld() == null) return;
        var block = loc.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        String abilityName = cached.abilityNames().get(rotationIndex % cached.abilityNames().size());

        var side = sign.getSide(Side.FRONT);
        side.setLine(0, "§l#" + position);
        side.setLine(1, truncate(cached.entry().name(), 15));
        side.setLine(2, ""+(int) cached.entry().rating());
        side.setLine(3, "§l" + truncate(abilityName, 15));

        sign.update();
    }

    private void clearSign(Location loc) {
        if (loc.getWorld() == null) return;
        var block = loc.getBlock();
        if (!(block.getState() instanceof Sign sign)) return;

        var side = sign.getSide(Side.FRONT);
        for (int i = 0; i < 4; i++) {
            side.setLine(i, "");
        }
        sign.update();
    }

    private String truncate(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}