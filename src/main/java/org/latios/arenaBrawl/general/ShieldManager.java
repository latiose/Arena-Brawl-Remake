package org.latios.arenaBrawl.general;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShieldManager {

    public record ActiveShield(double reductionPercent, long startedAt, long durationMillis, String title) {
        public boolean isExpired() {
            return System.currentTimeMillis() - startedAt >= durationMillis;
        }

        public long getRemainingMillis() {
            return Math.max(0, durationMillis - (System.currentTimeMillis() - startedAt));
        }
    }

    private final Map<UUID, List<ActiveShield>> activeShields = new HashMap<>();

    public void applyShield(Player player, double reductionPercent, long durationMillis, String title) {
        activeShields.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>())
                .add(new ActiveShield(reductionPercent, System.currentTimeMillis(), durationMillis, title));
    }

    public void tick() {
        if (activeShields.isEmpty()) return;

        Iterator<Map.Entry<UUID, List<ActiveShield>>> mapIterator = activeShields.entrySet().iterator();

        while (mapIterator.hasNext()) {
            Map.Entry<UUID, List<ActiveShield>> entry = mapIterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            List<ActiveShield> shields = entry.getValue();

            List<ActiveShield> expiredShields = new ArrayList<>();
            for (ActiveShield shield : shields) {
                if (shield.isExpired()) {
                    expiredShields.add(shield);
                }
            }

            if (!expiredShields.isEmpty()) {
                shields.removeAll(expiredShields);
                if (player != null && player.isOnline()) {
                    for (ActiveShield expired : expiredShields) {
                        Component expireMessage = Component.text("Your ", NamedTextColor.YELLOW)
                                .append(Component.text("§l"+expired.title(), NamedTextColor.AQUA))
                                .append(Component.text(" has expired!", NamedTextColor.YELLOW));
                        player.sendMessage(expireMessage);
                    }
                }
            }

            if (shields.isEmpty()) {
                mapIterator.remove();
            }
        }
    }

    public double getDamageReduction(Player player) {
        List<ActiveShield> shields = activeShields.get(player.getUniqueId());
        if (shields == null || shields.isEmpty()) return 0.0;

        shields.removeIf(ActiveShield::isExpired);

        if (shields.isEmpty()) {
            activeShields.remove(player.getUniqueId());
            return 0.0;
        }

        ActiveShield highestShield = null;
        for (ActiveShield shield : shields) {
            if (highestShield == null || shield.reductionPercent() > highestShield.reductionPercent()) {
                highestShield = shield;
            }
        }

        if (highestShield != null) {
            double progress = (double) highestShield.getRemainingMillis() / highestShield.durationMillis();
            StatusBarUtil.sendStatusBar(player, highestShield.title(), progress, "#00FFFF");
            return highestShield.reductionPercent();
        }

        return 0.0;
    }

    public void clear(Player player) {
        activeShields.remove(player.getUniqueId());
    }

    public boolean hasShield(Player player) {
        return getDamageReduction(player) > 0.0;
    }
}