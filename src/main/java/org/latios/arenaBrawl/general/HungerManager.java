package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;

public class HungerManager {

    public static final int MAX_HUNGER = 20;
    public static final int LOSS_WHILE_SPRINTING = 1;
    public static final int REGEN_WHILE_IDLE = 3;

    public void tick(Player player) {
        int current = player.getFoodLevel();
        int updated;

        if (player.isSprinting()) {
            updated = Math.max(0, current - LOSS_WHILE_SPRINTING);
        } else {
            updated = Math.min(MAX_HUNGER, current + REGEN_WHILE_IDLE);
        }

        player.setFoodLevel(updated);
    }

    public void reset(Player player) {
        player.setFoodLevel(MAX_HUNGER);
        player.setSaturation(0f);
    }
}