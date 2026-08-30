// abilities/utility/GolemFallAbility.java
package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class GolemFall implements Ability {

    private static final double MAX_RANGE = 20.0;
    private static final double SPAWN_HEIGHT_OFFSET = 10.0;
    private static final double IMPACT_RADIUS = 5.0;
    private static final double KNOCKBACK_STRENGTH = 5.0;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final StructureDemolitionService demolitionService;

    public GolemFall(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                     TeamManager teamManager, StructureDemolitionService demolitionService) {
        this.cost = new CooldownCost(cooldownManager, "golemfall", 30000, upgradeManager);
        this.teamManager = teamManager;
        this.demolitionService = demolitionService;
    }

    @Override
    public String getName() { return "Golem Fall"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Summons a golem above where you're aiming. On landing, it destroys nearby enemy "
                + "structures and knocks back enemies caught in the blast radius.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Range", String.valueOf(MAX_RANGE)),
                new AbilityStat("Impact Radius", String.valueOf(IMPACT_RADIUS)),
                new AbilityStat("Breaks structures", "Yes"),
                new AbilityStat("Knockback", "Extreme")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location target = resolveTargetLocation(player);
        Location spawnLocation = target.clone().add(0, SPAWN_HEIGHT_OFFSET, 0);

        IronGolem golem = spawnLocation.getWorld().spawn(spawnLocation, IronGolem.class, g -> {
            g.setAI(true);
            g.setInvulnerable(true);
            g.setSilent(false);
            g.setGravity(true);
            g.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, false, false));
        });

        EntityCleanupUtils.markAsArenaEntity(golem);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!golem.isValid() || golem.isDead()) {
                    cancel();
                    return;
                }

              //  golem.setVelocity(new Vector(0, -1.5, 0));

                Location loc = golem.getLocation();
                boolean hitGround = golem.isOnGround()
                        || loc.getBlock().getType().isSolid()
                        || loc.clone().subtract(0, 0.3, 0).getBlock().getType().isSolid();

                if (hitGround || ticks >= 40) {
                    triggerImpact(player, golem.getLocation());
                    golem.remove();
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    private Location resolveTargetLocation(Player player) {
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(), player.getEyeLocation().getDirection(), MAX_RANGE
        );

        if (result != null && result.getHitPosition() != null) {
            return result.getHitPosition().toLocation(player.getWorld());
        }

        Vector direction = player.getEyeLocation().getDirection().normalize().multiply(MAX_RANGE);
        return player.getEyeLocation().add(direction);
    }

    private void triggerImpact(Player caster, Location impactLocation) {
        impactLocation.getWorld().spawnParticle(Particle.EXPLOSION, impactLocation, 3);
        impactLocation.getWorld().playSound(impactLocation, Sound.BLOCK_ANVIL_LAND, 1.5f, 0.8f);
        impactLocation.getWorld().playSound(impactLocation, Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1f);

        List<PlacedStructure> nearbyStructures = demolitionService.findStructuresInRadius(impactLocation, IMPACT_RADIUS);
        for (PlacedStructure structure : nearbyStructures) {
            if (demolitionService.isEnemyStructure(caster, structure)) {
                demolitionService.demolish(structure, impactLocation);
            }
        }

        for (Entity nearby : impactLocation.getWorld().getNearbyEntities(impactLocation, IMPACT_RADIUS, IMPACT_RADIUS, IMPACT_RADIUS)) {
            if (nearby instanceof Player target && teamManager.isEnemy(caster, target)) {
                Vector direction = target.getLocation().toVector().subtract(impactLocation.toVector());

                if (direction.lengthSquared() == 0) {
                    direction = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5);
                }

                Vector knockback = direction.normalize().multiply(KNOCKBACK_STRENGTH);
                knockback.setY(1.2);

                target.setVelocity(knockback);
            }
        }
    }
}