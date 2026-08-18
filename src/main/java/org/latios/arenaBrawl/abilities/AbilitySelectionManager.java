package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilitySelectionManager {

    private final AbilityRegistry registry;
    private final Map<UUID, Map<AbilitySlot, String>> selections = new HashMap<>();

    public AbilitySelectionManager(AbilityRegistry registry) {
        this.registry = registry;
    }

    public void select(Player player, AbilitySlot slot, String abilityId) {
        selections
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(AbilitySlot.class))
                .put(slot, abilityId);
    }
    public String getSelection(Player player, AbilitySlot slot) {
        Map<AbilitySlot, String> playerSelections = selections.get(player.getUniqueId());
        if (playerSelections == null || !playerSelections.containsKey(slot)) {
            return registry.getDefault(slot);
        }
        return playerSelections.get(slot);
    }

    public void resetToDefaults(Player player) {
        selections.remove(player.getUniqueId());
    }
}