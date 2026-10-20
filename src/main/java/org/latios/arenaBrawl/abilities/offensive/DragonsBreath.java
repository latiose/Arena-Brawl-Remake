package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class DragonsBreath extends Breath {

    private final double damage;
    private final double tickDamage;
    private final double energyCost;
    private final long slowDurationTicks;
    private final double range;
    private final AbilityConfig config;

    public DragonsBreath(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                         CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        super(plugin, energyManager, config.getDouble("energy-cost", 60.0), teamManager, combatService, debuffManager, config.getMatchManager());
        this.damage = config.getDouble("damage", 140.0);
        this.tickDamage = config.getDouble("tick-damage", 25.0);
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.slowDurationTicks = config.getLong("slow-duration-ticks", 1000L);
        this.range = config.getDouble("range", 8.0);
        this.config = config;
    }

    @Override
    public String getName() { return "Dragons Breath"; }

    @Override
    protected Sound getCastSound() {
        return Sound.ENTITY_ENDER_DRAGON_GROWL;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.FLAME, point, 1, 0, 0, 0, 0);
        point.getWorld().spawnParticle(Particle.DRIPPING_WATER, point, 1, 0, 0, 0, 0);
    }

    @Override
    protected void applyHitEffects(Player target) {
        if (debuffManager != null) {
            debuffManager.tryApply(target, DebuffType.SLOW, slowDurationTicks);
        }
    }

    @Override
    public boolean activate(Player player) {
        player.getWorld().playSound(player.getLocation(), getCastSound(), 10.0f, 1.0f);

        List<Location> upright = conePoints(player, false);
        List<Location> hitboxPoints = new ArrayList<>(upright);
        hitboxPoints.addAll(conePoints(player, true));

        for (Player target : findHits(player, hitboxPoints)) {
            applyHit(player, target);
        }

        int total = upright.size();

        new BukkitRunnable() {
            int tick = 0;
            int spawned = 0;

            @Override
            public void run() {
                tick++;

                int targetIndex = Math.min((int) Math.ceil((double) tick / 20.0 * total), total);
                for (int i = spawned; i < targetIndex; i++) {
                    spawnTrailParticles(upright.get(i));
                }
                spawned = targetIndex;

                if (tick >= 20) {
                    for (Player target : findHits(player, hitboxPoints)) {
                        combatService.applyAbilityDamage(player, target, tickDamage, getName());
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Breathes in a cone in front of the user, dealing damage over the next second and slowing enemies caught inside";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Tick damage", String.valueOf((int) tickDamage)),
                new AbilityStat("Slow duration", String.valueOf((int) (slowDurationTicks / 1000L)) + "s"),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", String.valueOf((int) range))
        );
    }
}