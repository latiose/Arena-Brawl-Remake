package org.latios.arenaBrawl.hats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

import java.util.*;

public class HatPhraseListener {

    private static final double PHRASE_CHANCE = 0.20;
    private static final double HOLOGRAM_HEIGHT_OFFSET = 2.3;
    private static final long HOLOGRAM_LIFETIME_TICKS = 40;

    private final HatSelectionManager hatSelectionManager;
    private final Random random = new Random();

    private final Map<UUID, TextDisplay> activeHolograms = new HashMap<>();

    public HatPhraseListener(HatSelectionManager hatSelectionManager) {
        this.hatSelectionManager = hatSelectionManager;
    }

    public void onMeleeHit(Player victim) {
        HatDefinition hat = hatSelectionManager.getEquipped(victim);
        if (hat == null) return;

        if (random.nextDouble() >= PHRASE_CHANCE) return;

        List<String> phrases = hat.phrases();
        if (phrases.isEmpty()) return;

        String phrase = phrases.get(random.nextInt(phrases.size()));
        spawnHologram(victim, phrase);
    }

    private void spawnHologram(Player wearer, String phrase) {
        UUID playerId = wearer.getUniqueId();

        if (activeHolograms.containsKey(playerId)) {
            TextDisplay existing = activeHolograms.remove(playerId);
            if (existing != null && !existing.isDead()) {
                existing.remove();
            }
        }

        Location location = wearer.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0);
        TextColor color = TextColor.color(TextColor.color(0xFFA500));

        TextDisplay hologram = wearer.getWorld().spawn(location, TextDisplay.class, d -> {
            d.text(Component.text(phrase, color));
            d.setBillboard(Display.Billboard.CENTER);
            d.setSeeThrough(true);
            d.setShadowed(true);
            d.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
        });

        EntityCleanupUtils.markAsArenaEntity(hologram);

        activeHolograms.put(playerId, hologram);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (hologram.isDead() || !wearer.isOnline() || activeHolograms.get(playerId) != hologram) {
                    if (!hologram.isDead()) hologram.remove();
                    cancel();
                    return;
                }

                if (ticksElapsed >= HOLOGRAM_LIFETIME_TICKS) {
                    hologram.remove();
                    activeHolograms.remove(playerId, hologram);
                    cancel();
                    return;
                }

                hologram.teleport(wearer.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0));
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }
}