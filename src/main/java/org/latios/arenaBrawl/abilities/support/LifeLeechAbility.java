
package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LifeLeechAbility implements Ability {

    private static final int HIT_COUNT = 12;
    private static final long DURATION_MILLIS = 8_000;
    private static final long DURATION_TICKS = 160;

    private final AbilityCost cost;
    private final LifeLeechManager lifeLeechManager;

    public LifeLeechAbility(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                            LifeLeechManager lifeLeechManager) {
        this.cost = new CooldownCost(cooldownManager, "lifeleech", 30000, upgradeManager);
        this.lifeLeechManager = lifeLeechManager;
    }

    @Override
    public String getName() { return "Life Leech"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Heal off enemies by attacking them for some time.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", "8s"),
                new AbilityStat("Hits", "12"),
                new AbilityStat("Heal per hit", "60 HP")
        );
    }

    @Override
    public boolean activate(Player player) {
        lifeLeechManager.activate(player, HIT_COUNT, DURATION_MILLIS);
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1,1);
        startAmbientParticles(player);
        return true;
    }

    private void startAmbientParticles(Player player) {
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !lifeLeechManager.isActive(player) || ticksElapsed >= DURATION_TICKS) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);
                for (int i = 0; i < 3; i++) {
                    double offsetX = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
                    double offsetY = ThreadLocalRandom.current().nextDouble(-0.2, 0.6);
                    double offsetZ = ThreadLocalRandom.current().nextDouble(-0.6, 0.6);
                    loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(offsetX, offsetY, offsetZ), 1);
                }

                ticksElapsed += 5;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 5L);
    }
}