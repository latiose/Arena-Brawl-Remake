
package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilitySelectionManager {

    private final AbilityRegistry registry;
    private final AbilityPersistenceManager persistenceManager;
    private final Map<UUID, Map<AbilitySlot, String>> selections = new HashMap<>();

    public AbilitySelectionManager(AbilityRegistry registry, AbilityPersistenceManager persistenceManager) {
        this.registry = registry;
        this.persistenceManager = persistenceManager;
    }

    public void loadForPlayer(Player player) {
        Map<AbilitySlot, String> saved = persistenceManager.loadAll(player.getUniqueId());
        if (!saved.isEmpty()) {
            selections.put(player.getUniqueId(), new EnumMap<>(saved));
        }
    }

    public void select(Player player, AbilitySlot slot, String abilityId) {
        selections
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(AbilitySlot.class))
                .put(slot, abilityId);

        persistenceManager.save(player, slot, abilityId);
    }

    public String getSelection(Player player, AbilitySlot slot) {
        Map<AbilitySlot, String> playerSelections = selections.get(player.getUniqueId());
        if (playerSelections == null || !playerSelections.containsKey(slot)) {
            return registry.getDefault(slot);
        }
        return playerSelections.get(slot);
    }

    public void unloadPlayer(Player player) {
        selections.remove(player.getUniqueId());
    }
}