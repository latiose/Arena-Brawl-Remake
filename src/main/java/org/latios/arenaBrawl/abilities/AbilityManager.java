package org.latios.arenaBrawl.abilities;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.UnknownNullability;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {

    private final Map<UUID, Map<AbilitySlot, Ability>> playerAbilities = new HashMap<>();

    public void setAbility(Player player, AbilitySlot slot, Ability ability) {
        playerAbilities
                .computeIfAbsent(player.getUniqueId(), k -> new EnumMap<>(AbilitySlot.class))
                .put(slot, ability);
    }

    public void clearAbilities(Player player) {
        playerAbilities.remove(player.getUniqueId());
    }

    public void tryActivate(Player player, AbilitySlot slot) {
        Map<AbilitySlot, Ability> abilities = playerAbilities.get(player.getUniqueId());
        if (abilities == null || !abilities.containsKey(slot)) {
            player.sendMessage("§cYou have no ability assigned to that slot.");
            return;
        }

        Ability ability = abilities.get(slot);
        AbilityCost cost = ability.getCost();

        if (!cost.canPay(player)) {
            player.sendMessage("§cYou can't use " + ability.getName()
                    + " yet (" + cost.describeRemaining(player) + ")");
            return;
        }

        boolean success = ability.activate(player);

        if (success) {
            cost.pay(player);
        }
    }

    public Ability getAbility(Player player, AbilitySlot slot) {
        Map<AbilitySlot, Ability> abilities = playerAbilities.get(player.getUniqueId());
        return abilities != null ? abilities.get(slot) : null;
    }
}
