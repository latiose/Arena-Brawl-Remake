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
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SongOfPowerAbility implements Ability {

    private static final long DURATION_MILLIS = 7_000;
    private static final long DURATION_TICKS = 140;
    private static final double RADIUS = 6.0;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final SongOfPowerManager songOfPowerManager;
    private final EnergyModifierManager energyModifierManager;
    private final DebuffManager debuffManager;

    public SongOfPowerAbility(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                              TeamManager teamManager, SongOfPowerManager songOfPowerManager,
                              EnergyModifierManager energyModifierManager, DebuffManager debuffManager) {
        this.cost = new CooldownCost(cooldownManager, "songofpower", 45000, upgradeManager);
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
        return "For 7 seconds, you and allies within 6 blocks gain double energy regeneration, "
                + "stop losing hunger, and become immune to negative status effects.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", "7s"),
                new AbilityStat("Radius", "6 blocks"),
                new AbilityStat("Energy regen", "x2"),
                new AbilityStat("Debuff immunity", "Yes")
        );
    }

    @Override
    public boolean activate(Player player) {
        debuffManager.clear(player);
        debuffManager.setSongOfPowerManager(songOfPowerManager);
        Set<Player> affected = new HashSet<>();
        affected.add(player);

        for (Entity nearby : player.getNearbyEntities(RADIUS, RADIUS, RADIUS)) {
            if (nearby instanceof Player ally && teamManager.isAlly(player, ally)) {
                affected.add(ally);
            }
        }

        for (Player target : affected) {
            songOfPowerManager.applyBuff(target, DURATION_MILLIS);
            debuffManager.clear(target);
            energyModifierManager.addModifier(target, "song_of_power", 2.0, DURATION_MILLIS);
        }

        startAmbientEffect(player);
        return true;
    }

    private void startAmbientEffect(Player caster) {
        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || ticksElapsed >= DURATION_TICKS) {
                    cancel();
                    return;
                }
                Location loc = caster.getLocation();
                for (int i = 0; i < 5; i++) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.5f);
                    loc.getWorld().spawnParticle(Particle.NOTE, loc.clone().add(0, 1.5, 0), 1, 0.5, 0.3, 0.5, 1.0);
                }
                ticksElapsed += 20;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 20L);
    }
}