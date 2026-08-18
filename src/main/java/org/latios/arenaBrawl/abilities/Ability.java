package org.latios.arenaBrawl.abilities;



import org.bukkit.entity.Player;

public interface Ability {
        String getName();
        AbilityCost getCost();
        void activate(Player player);
        default void onMatchStart(Player player) {}
}