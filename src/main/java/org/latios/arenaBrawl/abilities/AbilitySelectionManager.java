
package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AbilitySelectionManager {

    /** Special selection id: a random ability is rolled for each match. */
    public static final String RANDOM_ID = "random";

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

        String selected = playerSelections.get(slot);
        if (selected == null) {
            return registry.getDefault(slot);
        }
        // "random" is a pseudo-selection: it is resolved to a concrete ability
        // per match (see resolveSelection), so it is valid even though no
        // ability is registered under that id.
        if (RANDOM_ID.equals(selected)) {
            return RANDOM_ID;
        }
        // A saved ability can be removed from the registry (e.g. a retired
        // ability); fall back to the slot default instead of letting match
        // creation fail on an unknown id.
        if (!registry.getAvailableIds(slot).contains(selected)) {
            return registry.getDefault(slot);
        }
        return selected;
    }

    /** Returns true if the slot is set to the special "random" selection. */
    public boolean isRandom(Player player, AbilitySlot slot) {
        return RANDOM_ID.equals(getSelection(player, slot));
    }

    /**
     * Resolves the current selection to a concrete ability id for a match.
     * A "random" selection rolls a different ability from the slot every
     * time this is called (i.e. every match start).
     */
    public String resolveSelection(Player player, AbilitySlot slot) {
        String selected = getSelection(player, slot);
        if (!RANDOM_ID.equals(selected)) {
            return selected;
        }
        List<String> ids = new ArrayList<>(registry.getAvailableIds(slot));
        if (ids.isEmpty()) {
            return registry.getDefault(slot);
        }
        return ids.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(ids.size()));
    }

    public void unloadPlayer(Player player) {
        selections.remove(player.getUniqueId());
    }
}