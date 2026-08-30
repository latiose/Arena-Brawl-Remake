package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;


import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BurstFire implements Ability {

    private static final double DAMAGE = 55.0;
    private static final double MAX_RANGE = 5.0;
    private static final int ENERGY_COST = 20;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Plugin plugin;

    public BurstFire(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                     CombatService combatService) {
        this.plugin = plugin;
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Burst Fire"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getEyeLocation().subtract(0, 0.2, 0);
        Vector direction = startLoc.getDirection().normalize();

        player.getWorld().playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.8f);
        player.getWorld().playSound(startLoc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f);

        new BukkitRunnable() {
            int shotsFired = 0;

            @Override
            public void run() {
                if (shotsFired >= 3 || !player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                fireSingleRay(player, startLoc.clone(), direction.clone());
                shotsFired++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void fireSingleRay(Player caster, Location origin, Vector direction) {
        double spreadX = ThreadLocalRandom.current().nextDouble(-0.04, 0.04);
        double spreadY = ThreadLocalRandom.current().nextDouble(-0.04, 0.04);
        double spreadZ = ThreadLocalRandom.current().nextDouble(-0.04, 0.04);

        Vector finalDir = direction.add(new Vector(spreadX, spreadY, spreadZ)).normalize();
        double step = 0.3;

        for (double traveled = 0; traveled <= MAX_RANGE; traveled += step) {
            Location point = origin.add(finalDir.clone().multiply(step));

            if (point.getBlock().getType().isSolid()) {
                point.getWorld().spawnParticle(Particle.CRIT, point, 5, 0.1, 0.1, 0.1, 0.05);
                break;
            }
            point.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0, 0, 0, 0.01);
            point.getWorld().spawnParticle(Particle.WAX_OFF, point, 1, 0, 0, 0, 0);

            for (Player target : point.getWorld().getPlayers()) {
                if (target.equals(caster) || !teamManager.isEnemy(caster, target) || target.isDead()) {
                    continue;
                }

                if (target.getBoundingBox().expand(0.2, 0.2, 0.2).contains(point.getX(), point.getY(), point.getZ())) {
                    combatService.applyAbilityDamage(caster, target, DAMAGE / 3.0, getName(), point);

                    point.getWorld().playSound(point, Sound.ENTITY_PLAYER_HURT, 0.8f, 1.5f);
                    return;
                }
            }
        }
    }

    @Override
    public String getDescription() {
        return "Fires a rapid short-range burst of electric sparks, dealing damage to the first enemy hit.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Range", (int) MAX_RANGE + "m"),
                new AbilityStat("Energy Cost", String.valueOf(ENERGY_COST))
        );
    }
}