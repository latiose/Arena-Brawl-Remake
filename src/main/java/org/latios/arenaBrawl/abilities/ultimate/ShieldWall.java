package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShieldWall implements Ability {

    private static final double DAMAGE_REDUCTION = 0.70;
    private static final long DURATION_MILLIS = 10_000;
    private static final long CHARGE_TIME_MILLIS = 60_000;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final Map<UUID, Long> activeUntil = new HashMap<>();

    public ShieldWall(CooldownManager cooldownManager, UsageManager usageManager) {
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "shieldwall");
    }

    @Override
    public String getName() { return "Shield Wall"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "shieldwall", CHARGE_TIME_MILLIS);
    }

    @Override
    public void activate(Player player) {
        activeUntil.put(player.getUniqueId(), System.currentTimeMillis() + DURATION_MILLIS);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 40, 0.5, 1, 0.5);
    }

    public double getDamageReduction(Player player) {
        Long expiresAt = activeUntil.get(player.getUniqueId());
        if (expiresAt == null) return 0.0;
        if (System.currentTimeMillis() > expiresAt) {
            activeUntil.remove(player.getUniqueId());
            return 0.0;
        }
        return DAMAGE_REDUCTION;
    }
}