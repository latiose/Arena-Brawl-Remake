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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LifeLeech implements Ability {

    private final int hitCount;
    private final long durationMillis;
    private final long cooldownMs;
    private final double healPerHit;

    private final AbilityCost cost;
    private final LifeLeechManager lifeLeechManager;

    public LifeLeech(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     LifeLeechManager lifeLeechManager, AbilityConfig config) {
        this.hitCount = config.getInt("hit-count", 12);
        this.durationMillis = config.getLong("duration-millis", 8000L);
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.healPerHit = config.getDouble("heal-per-hit", 60.0);

        this.cost = new CooldownCost(cooldownManager, "lifeleech", cooldownMs, upgradeManager);
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
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Hits", String.valueOf(hitCount)),
                new AbilityStat("Heal per hit", (int) healPerHit + " HP"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        lifeLeechManager.activate(player, hitCount, durationMillis);
        player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_HURT, 1.0f, 1.0f);
        startAmbientParticles(player);
        return true;
    }

    private void startAmbientParticles(Player player) {
        final long durationTicks = (durationMillis / 1000L) * 20L;
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !lifeLeechManager.isActive(player) || ticksElapsed >= durationTicks) {
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