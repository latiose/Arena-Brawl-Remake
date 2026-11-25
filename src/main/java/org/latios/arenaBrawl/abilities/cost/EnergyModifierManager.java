package org.latios.arenaBrawl.abilities.cost;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.StatusBarUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnergyModifierManager {

    private static final String BAR_COLOR = "#00BFFF";

    private final Map<UUID, Map<String, ModifierEntry>> activeModifiers = new HashMap<>();

    private static class ModifierEntry {
        final double multiplier;
        final long startedAt;
        final long durationMillis;
        final long expiresAt;
        final String title;

        ModifierEntry(double multiplier, long durationMillis, String title) {
            this.multiplier = multiplier;
            this.startedAt = System.currentTimeMillis();
            this.durationMillis = durationMillis;
            this.expiresAt = startedAt + durationMillis;
            this.title = title.replace('_', ' ').toUpperCase();
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        long getRemainingMillis() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }
    }

    public void addModifier(Player player, String id, double multiplier, long durationMillis) {
        addModifier(player, id, multiplier, durationMillis, id);
    }

    public void addModifier(Player player, String id, double multiplier, long durationMillis, String title) {
        activeModifiers
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .put(id, new ModifierEntry(multiplier, durationMillis, title));
    }

    public boolean hasModifier(Player player, String id) {
        Map<String, ModifierEntry> map = activeModifiers.get(player.getUniqueId());
        if (map == null) return false;

        ModifierEntry entry = map.get(id);
        if (entry == null) return false;

        if (entry.isExpired()) {
            map.remove(id);
            notifyExpired(player, entry);
            return false;
        }
        return true;
    }

    public double getTotalMultiplier(Player player) {
        Map<String, ModifierEntry> map = activeModifiers.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return 1.0;

        double maxMultiplier = 1.0;
        double negativePenalties = 0.0;
        ModifierEntry bestPositive = null;
        ModifierEntry worstNegative = null;

        var iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ModifierEntry mod = entry.getValue();

            if (mod.isExpired()) {
                iterator.remove();
                notifyExpired(player, mod);
            } else if (mod.multiplier >= 1.0) {
                maxMultiplier = Math.max(maxMultiplier, mod.multiplier);
                if (bestPositive == null || mod.multiplier > bestPositive.multiplier) {
                    bestPositive = mod;
                }
            } else {
                negativePenalties += (1.0 - mod.multiplier);
                if (worstNegative == null || mod.multiplier < worstNegative.multiplier) {
                    worstNegative = mod;
                }
            }
        }

        if (map.isEmpty()) {
            activeModifiers.remove(player.getUniqueId());
            return 1.0;
        }

        ModifierEntry toDisplay = (bestPositive != null) ? bestPositive : worstNegative;
        if (toDisplay != null) {
            double progress = (double) toDisplay.getRemainingMillis() / toDisplay.durationMillis;
            StatusBarUtil.sendStatusBar(player, toDisplay.title, progress, BAR_COLOR);
        }

        return maxMultiplier - negativePenalties;
    }

    private void notifyExpired(Player player, ModifierEntry entry) {
        Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                .append(Component.text(entry.title, NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text(" has expired!", NamedTextColor.YELLOW));
        player.sendMessage(expireMessage);
    }

    public void clear(Player player) {
        activeModifiers.remove(player.getUniqueId());
    }
}