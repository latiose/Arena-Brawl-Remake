package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongOfPower implements Ability {

    private final long durationMillis;
    private final double radius;
    private final long cooldownMs;
    private final double energyMultiplier;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final SongOfPowerManager songOfPowerManager;
    private final EnergyModifierManager energyModifierManager;
    private final DebuffManager debuffManager;

    public SongOfPower(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                       TeamManager teamManager, SongOfPowerManager songOfPowerManager,
                       EnergyModifierManager energyModifierManager, DebuffManager debuffManager,
                       AbilityConfig config) {
        this.durationMillis = config.getLong("duration-millis", 7000L);
        this.radius = config.getDouble("radius", 6.0);
        this.cooldownMs = config.getLong("cooldown-ms", 45000L);
        this.energyMultiplier = config.getDouble("energy-multiplier", 2.0);

        this.cost = new CooldownCost(cooldownManager, "songofpower", cooldownMs, upgradeManager);
        this.teamManager = teamManager;
        this.songOfPowerManager = songOfPowerManager;
        this.energyModifierManager = energyModifierManager;
        this.debuffManager = debuffManager;
    }

    @Override
    public String getName() { return "Song of Power"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "For its duration, you and allies within " + (int) radius + " blocks gain double energy regeneration, "
                + "stop losing hunger, and become immune to negative status effects.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMillis / 1000L) + "s"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Energy regen", "x" + (int) energyMultiplier),
                new AbilityStat("Debuff immunity", "Yes"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        debuffManager.clear(player);
        Set<Player> affected = new HashSet<>();
        affected.add(player);

        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Player ally && teamManager.isAlly(player, ally)) {
                affected.add(ally);
            }
        }

        for (Player target : affected) {
            songOfPowerManager.applyBuff(target, durationMillis);
            debuffManager.clear(target);
            energyModifierManager.addModifier(target, "song_of_power", energyMultiplier, durationMillis);
        }

        startAmbientEffect(player);
        return true;
    }

    private void startAmbientEffect(Player caster) {
        final long durationTicks = (durationMillis / 1000L) * 20L;
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || ticksElapsed >= durationTicks) {
                    cancel();
                    return;
                }
                Location loc = caster.getLocation();
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.2f);
                loc.getWorld().spawnParticle(Particle.NOTE, loc.clone().add(0, 1.5, 0), 4, 0.5, 0.3, 0.5, 0.1);

                ticksElapsed += 20;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 20L);
    }
}