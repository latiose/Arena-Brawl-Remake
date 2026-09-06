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

    public void tryActivate(Player player, AbilitySlot slot) {
        Map<AbilitySlot, Ability> abilities = playerAbilities.get(player.getUniqueId());
        if (abilities == null || !abilities.containsKey(slot)) {
            player.sendMessage("§cYou have no ability assigned to that slot.");
            return;
        }
        if(player.getGameMode() ==  GameMode.SPECTATOR) {
            return;
        }
        Ability ability = abilities.get(slot);
        AbilityCost cost = ability.getCost();

        if (!cost.canPay(player) && (cost instanceof CooldownCost)) {
            player.sendMessage("§eWait another " + cost.describeRemaining(player));
            return;
        }
        else if(!cost.canPay(player) && cost instanceof EnergyCost){
            player.sendMessage("§e" + cost.describeRemaining(player));
            return;
        }
        else if(!cost.canPay(player) && cost instanceof UltimateCost){
            if(cost.isPermanentlyUnavailable(player)){
                player.sendMessage("§e" + cost.describeRemaining(player));
            }
          else{
                player.sendMessage("§eWait another " + cost.describeRemaining(player));
            }
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
