package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SugarRush implements Ability {

    private static final long COOLDOWN_MILLIS = 30_000;
    private static final int SPEED_AMPLIFIER = 2;
    private static final int SPEED_DURATION_TICKS = 80;
    private static final long SLOW_DURATION_MS = 3_000;

    private final AbilityCost cost;
    private final DebuffManager debuffManager;

    public SugarRush(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager, DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "sugarrush", COOLDOWN_MILLIS, combatUpgradeManager);
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Sugar rush"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED, SPEED_DURATION_TICKS, SPEED_AMPLIFIER, true, false
        ));
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    debuffManager.tryApply(player, DebuffType.SLOW, SLOW_DURATION_MS);
                    player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_BREATH, 1f, 0.8f);
                }
            }
        }.runTaskLater(ArenaBrawlPlugin.getInstance(), SPEED_DURATION_TICKS);

        return true;
    }

    @Override
    public String getDescription() {
        return "Gives the user speed, then crashes giving them slow";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Cooldown", (COOLDOWN_MILLIS / 1000) + "s"),
                new AbilityStat("Speed duration", (SPEED_DURATION_TICKS / 20) + "s"),
                new AbilityStat("Speed Level", String.valueOf(SPEED_AMPLIFIER + 1)),
                new AbilityStat("Slow duration", (SLOW_DURATION_MS / 1000) + "s")
        );
    }
}