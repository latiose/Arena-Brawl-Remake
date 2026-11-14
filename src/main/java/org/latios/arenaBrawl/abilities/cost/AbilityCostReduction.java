package org.latios.arenaBrawl.abilities.cost;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.AbilitySlot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityCostReduction {

    private static final Map<UUID, Map<AbilitySlot, Double>> activeReductions = new HashMap<>();

    private AbilityCostReduction() {
    }

    public static void apply(Player player, AbilitySlot slot, double reductionPercent) {
        double clamped = Math.max(0.0, Math.min(1.0, reductionPercent));
        activeReductions.computeIfAbsent(player.getUniqueId(), ignored -> new EnumMap<>(AbilitySlot.class))
                .put(slot, clamped);
    }

    public static double getReduction(Player player, AbilitySlot slot) {
        Map<AbilitySlot, Double> reductions = activeReductions.get(player.getUniqueId());
        if (reductions == null) return 0.0;
        return reductions.getOrDefault(slot, 0.0);
    }

    public static double getMultiplier(Player player, AbilitySlot slot) {
        return 1.0 - getReduction(player, slot);
    }

    public static boolean hasReduction(Player player, AbilitySlot slot) {
        return getReduction(player, slot) > 0.0;
    }

    public static void clear(Player player) {
        activeReductions.remove(player.getUniqueId());
    }
}
