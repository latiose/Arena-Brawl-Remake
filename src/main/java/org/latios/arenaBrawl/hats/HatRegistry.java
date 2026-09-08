package org.latios.arenaBrawl.hats;

import java.util.*;

public class HatRegistry {

    private final Map<String, HatDefinition> hatsById = new LinkedHashMap<>();
    private final Map<HatRarity, List<HatDefinition>> hatsByRarity = new EnumMap<>(HatRarity.class);

    public HatRegistry() {
        for (HatRarity rarity : HatRarity.values()) {
            hatsByRarity.put(rarity, new ArrayList<>());
        }
    }

    public void clear() {
        hatsById.clear();
        for (List<HatDefinition> list : hatsByRarity.values()) {
            list.clear();
        }
    }

    public void register(HatDefinition hat) {
        hatsById.put(hat.id(), hat);
        hatsByRarity.get(hat.rarity()).add(hat);
    }
    public HatDefinition get(String id) {
        return hatsById.get(id);
    }

    public List<HatDefinition> getByRarity(HatRarity rarity) {
        return hatsByRarity.getOrDefault(rarity, Collections.emptyList());
    }

    public Collection<HatDefinition> getAllHats() {
        return hatsById.values();
    }
}