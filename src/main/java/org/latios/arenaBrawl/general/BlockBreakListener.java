package org.latios.arenaBrawl.general;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private static boolean breakProtectionEnabled = true;

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (breakProtectionEnabled) {
            event.setCancelled(true);
        }
    }

    public static boolean isBreakProtectionEnabled() {
        return breakProtectionEnabled;
    }

    public static void setBreakProtectionEnabled(boolean enabled) {
        breakProtectionEnabled = enabled;
    }
}