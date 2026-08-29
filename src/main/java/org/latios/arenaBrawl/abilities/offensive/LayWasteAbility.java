package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LayWasteAbility implements Ability {

    private static final double DAMAGE = 150.0;
    private static final double ENERGY_COST = 40.0;
    private static final double MAX_TARGET_DISTANCE = 15.0;
    private static final double RADIUS = 1.0;
    private static final long DELAY_TICKS = 15L;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public LayWasteAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Lay Waste"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Marks a targeted block that explodes after 0.75 seconds, dealing damage to enemies in the area.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", "120"),
                new AbilityStat("Energy Cost", "40"),
                new AbilityStat("Delay", "0.75s"),
                new AbilityStat("Radius", RADIUS + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                MAX_TARGET_DISTANCE,
                FluidCollisionMode.NEVER,
                true
        );

        if (result == null || result.getHitBlock() == null) {
            player.sendMessage("§cYou must target a block on the ground.");
            return false;
        }

        Block hitBlock = result.getHitBlock();

        if (result.getHitBlockFace() != null && result.getHitBlockFace().getModY() < 0) {
            player.sendMessage("§cYou must target a block on the ground.");
            return false;
        }

        Location targetLocation = result.getHitPosition().toLocation(player.getWorld());

        player.getWorld().playSound(targetLocation, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1.0f, 1.8f);

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed++;

                if (ticksElapsed < DELAY_TICKS) {
                    drawIndicatorCircle(targetLocation, RADIUS);
                    if (ticksElapsed % 3 == 0) {
                        targetLocation.getWorld().playSound(targetLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 2.0f);
                    }
                    return;
                }

                cancel();

                targetLocation.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, targetLocation, 35, 0.6, 0.4, 0.6, 0.08);
                targetLocation.getWorld().spawnParticle(Particle.EXPLOSION, targetLocation, 2, 0.2, 0.2, 0.2, 0.0);
                targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.8f);
                targetLocation.getWorld().playSound(targetLocation, Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 1.2f);

                Set<Player> targetsToHit = new HashSet<>();
                for (Entity entity : targetLocation.getWorld().getNearbyEntities(targetLocation, RADIUS, RADIUS + 1.5, RADIUS)) {
                    if (entity instanceof Player victim && !victim.equals(player)) {
                        if (teamManager.isEnemy(player, victim)) {
                            targetsToHit.add(victim);
                        }
                    }
                }

                for (Player victim : targetsToHit) {
                    combatService.applyAbilityDamage(player, victim, DAMAGE, getName(), targetLocation);
                }
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private void drawIndicatorCircle(Location center, double radius) {
        int points = 20;
        for (int i = 0; i < points; i++) {
            double angle = i * (2 * Math.PI / points);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location pLoc = center.clone().add(x, 0.05, z);
            pLoc.getWorld().spawnParticle(
                    Particle.DUST,
                    pLoc,
                    1,
                    new Particle.DustOptions(org.bukkit.Color.fromRGB(220, 40, 40), 1.0f)
            );
        }
    }
}