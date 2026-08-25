package org.latios.arenaBrawl.rating;

import java.util.UUID;

public record LeaderboardEntry(UUID uuid, String name, double rating) {}