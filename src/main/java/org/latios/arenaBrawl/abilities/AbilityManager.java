package org.latios.arenaBrawl.abilities;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

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

    private final Map<UUID, Integer> lastTick = new HashMap<>();

    public boolean tryActivate(Player player, AbilitySlot slot) {
        if (player.getGameMode() == GameMode.SPECTATOR) return false;

        Map<AbilitySlot, Ability> abilities = playerAbilities.get(player.getUniqueId());
        if (abilities == null || !abilities.containsKey(slot)) {
            player.sendMessage("§cYou have no ability assigned to that slot.");
            return false;
        }

        int now = org.bukkit.Bukkit.getCurrentTick();
        Integer last = lastTick.put(player.getUniqueId(), now);
        if (last != null && last == now) return false;

        Ability ability = abilities.get(slot);
        AbilityCost cost = ability.getCost();

        if (!cost.canPay(player)) {
            if (cost instanceof EnergyCost) {
                player.sendMessage("§e" + cost.describeRemaining(player));
            } else if (cost instanceof UltimateCost && cost.isPermanentlyUnavailable(player)) {
                player.sendMessage("§e" + cost.describeRemaining(player));
            } else {
                player.sendMessage("§eWait another " + cost.describeRemaining(player));
            }
            return false;
        }

        if (!ability.activate(player)) return false;
        cost.pay(player);
        return true;
    }

    public Ability getAbility(Player player, AbilitySlot slot) {
        Map<AbilitySlot, Ability> abilities = playerAbilities.get(player.getUniqueId());
        return abilities != null ? abilities.get(slot) : null;
    }
}
