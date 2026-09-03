package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.ShieldManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class DodgeRoll implements Ability {

    private final long cooldownMs;
    private final double dashSpeed;
    private final long iframeDurationMs;
    private final AbilityCost cost;
    private final ShieldManager shieldManager;
    private final Plugin plugin;

    public DodgeRoll(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     ShieldManager shieldManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 22000L);
        this.dashSpeed = config.getDouble("dash-speed", 1.3);
        this.iframeDurationMs = config.getLong("iframe-duration-ms", 600L);
        this.cost = new CooldownCost(cooldownManager, "dodgeroll", cooldownMs, upgradeManager);
        this.shieldManager = shieldManager;
    }

    @Override
    public String getName() {
        return "Dodge Roll";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Roll forward in the direction you are looking, gaining temporary invulnerability.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Invulnerability", (iframeDurationMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().normalize().setY(0.15).multiply(dashSpeed);
        player.setVelocity(direction);

        shieldManager.applyShield(player, 1.0, iframeDurationMs, "I-FRAMES");

        player.setFallDistance(0);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 0.2, 0), 12, 0.3, 0.1, 0.3, 0.02);

        long delayTicks = iframeDurationMs / 50L;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_FALL, 0.8f, 1.2f);
                player.getWorld().spawnParticle(Particle.DUST_PLUME, player.getLocation().add(0, 0.1, 0), 8, 0.2, 0.0, 0.2, 0.01);
            }
        }, delayTicks);

        return true;
    }
}