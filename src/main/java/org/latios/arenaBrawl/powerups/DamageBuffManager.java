package org.latios.arenaBrawl.powerups;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DamageBuffManager {

    private record ActiveBuff(double multiplier, long startedAt, long durationMillis) {}

    private final Map<UUID, ActiveBuff> activeBuffs = new HashMap<>();
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public void applyBuff(Player player, double multiplier, long durationMillis) {
        long now = System.currentTimeMillis();
        activeBuffs.put(player.getUniqueId(), new ActiveBuff(multiplier, now, durationMillis));

        BossBar bar = Bukkit.createBossBar("§c§lDOUBLE DAMAGE", BarColor.PURPLE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        bossBars.put(player.getUniqueId(), bar);
    }

    public double getMultiplier(Player player) {
        ActiveBuff buff = activeBuffs.get(player.getUniqueId());
        if (buff == null) return 1.0;

        long elapsed = System.currentTimeMillis() - buff.startedAt();
        long remaining = buff.durationMillis() - elapsed;

        if (remaining <= 0) {
            clear(player);
            player.sendMessage(
                    Component.text("Your double damage powerup has expired!", NamedTextColor.RED, TextDecoration.BOLD)
            );
            return 1.0;
        }

        BossBar bar = bossBars.get(player.getUniqueId());
        if (bar != null) {
            double progress = (double) remaining / buff.durationMillis();
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        }

        return buff.multiplier();
    }


    public void clear(Player player) {
        activeBuffs.remove(player.getUniqueId());
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}