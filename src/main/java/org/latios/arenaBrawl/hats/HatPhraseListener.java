// hats/HatPhraseListener.java
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

import java.util.List;
import java.util.Random;

public class HatPhraseListener {

    private static final double PHRASE_CHANCE = 0.20;
    private static final double HOLOGRAM_HEIGHT_OFFSET = 2.3;
    private static final long HOLOGRAM_LIFETIME_TICKS = 40;

    private final HatSelectionManager hatSelectionManager;
    private final Random random = new Random();

    public HatPhraseListener(HatSelectionManager hatSelectionManager) {
        this.hatSelectionManager = hatSelectionManager;
    }

    /**
     * Call this whenever a player successfully lands a melee hit on another player.
     * The phrase comes from the VICTIM's equipped hat (as if the hat "reacts" to being hit),
     * and floats above the victim, not the attacker.
     */
    public void onMeleeHit(Player attacker, Player victim) {
        HatDefinition hat = hatSelectionManager.getEquipped(victim);
        if (hat == null) return;

        if (random.nextDouble() >= PHRASE_CHANCE) return;

        List<String> phrases = hat.phrases();
        if (phrases.isEmpty()) return;

        String phrase = phrases.get(random.nextInt(phrases.size()));
        spawnHologram(victim, hat, phrase);
    }

    private void spawnHologram(Player wearer, HatDefinition hat, String phrase) {
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

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (hologram.isDead() || !wearer.isOnline()) {
                    if (!hologram.isDead()) hologram.remove();
                    cancel();
                    return;
                }

                if (ticksElapsed >= HOLOGRAM_LIFETIME_TICKS) {
                    hologram.remove();
                    cancel();
                    return;
                }

                hologram.teleport(wearer.getLocation().add(0, HOLOGRAM_HEIGHT_OFFSET, 0));
                ticksElapsed++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
    }
}