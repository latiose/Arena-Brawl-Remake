
package org.latios.arenaBrawl.hats;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.stats.StatsManager;

import java.util.List;
import java.util.Random;

public class MagicChestManager {

    public sealed interface ChestResult permits CoinsResult, HatResult {}
    public record CoinsResult(int amount) implements ChestResult {}
    public record HatResult(HatDefinition hat, boolean wasNew) implements ChestResult {}

    private final StatsManager statsManager;
    private final HatRegistry hatRegistry;
    private final HatSelectionManager hatSelectionManager;
    private final Random random = new Random();

    public MagicChestManager(StatsManager statsManager, HatRegistry hatRegistry, HatSelectionManager hatSelectionManager) {
        this.statsManager = statsManager;
        this.hatRegistry = hatRegistry;
        this.hatSelectionManager = hatSelectionManager;
    }

    public ChestResult open(Player player) {
        double roll = random.nextDouble();

        if (roll < 0.50) {
            int amount = 50 + random.nextInt(151); // 50 to 200 inclusive
            var stats = statsManager.getStats(player);
            stats.coins += amount;
            statsManager.saveDirectly(player, stats);
            return new CoinsResult(amount);
        }

        HatRarity rarity;
        if (roll < 0.90) {
            rarity = HatRarity.COMMON; // 0.50 to 0.90 -> 40%
        } else if (roll < 0.98) {
            rarity = HatRarity.RARE;   // 0.90 to 0.98 -> 8%
        } else {
            rarity = HatRarity.EPIC;   // 0.98 to 1.00 -> 2%
        }

        List<HatDefinition> pool = hatRegistry.getByRarity(rarity);
        if (pool.isEmpty()) {
            // Fallback if a rarity has no hats registered: give coins instead
            int amount = 50 + random.nextInt(151);
            var stats = statsManager.getStats(player);
            stats.coins += amount;
            statsManager.saveDirectly(player, stats);
            return new CoinsResult(amount);
        }

        HatDefinition hat = pool.get(random.nextInt(pool.size()));
        boolean wasNew = hatSelectionManager.unlock(player, hat.id());
        return new HatResult(hat, wasNew);
    }
}