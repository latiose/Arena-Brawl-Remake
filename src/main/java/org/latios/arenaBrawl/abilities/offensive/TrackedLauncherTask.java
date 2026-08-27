package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

public class TrackedLauncherTask extends BukkitRunnable {

    private final ArmorStand armorStand;
    private final Player shooter;
    private final double mainDamage;
    private final double sliceDamage;
    private final double aoeRadius;
    private final String abilityName;
    private final Material sliceMaterial;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private Vector velocity;
    private int ticksLived = 0;

    public TrackedLauncherTask(ArmorStand armorStand, Vector velocity, Player shooter, double mainDamage, double sliceDamage,
                               double aoeRadius, String abilityName, Material headMaterial, Material sliceMaterial,
                               TeamManager teamManager, CombatService combatService) {
        this.armorStand = armorStand;
        this.velocity = velocity;
        this.shooter = shooter;
        this.mainDamage = mainDamage;
        this.sliceDamage = sliceDamage;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.sliceMaterial = sliceMaterial;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public void run() {
        ticksLived++;

        if (!armorStand.isValid() || ticksLived > 100) {
            cancel();
            armorStand.remove();
            return;
        }

        velocity.setY(velocity.getY() - 0.04);
        Location nextLoc = armorStand.getLocation().add(velocity);
        armorStand.teleport(nextLoc);

        Location projectileCenter = nextLoc.clone().add(0, 1.4, 0);

        for (Player candidate : armorStand.getWorld().getPlayers()) {
            if (candidate.equals(shooter) || !teamManager.isEnemy(shooter, candidate)) continue;

            if (candidate.getBoundingBox().expand(0.5).contains(projectileCenter.toVector())) {
                explode(candidate, projectileCenter);
                return;
            }
        }

        if (projectileCenter.getBlock().getType().isSolid() && !projectileCenter.getBlock().isPassable()) {
            explode(null, projectileCenter);
        }
    }

    private void explode(Player directHitVictim, Location impactLoc) {
        cancel();

        if (directHitVictim != null) {
            combatService.applyAbilityDamage(shooter, directHitVictim, mainDamage, abilityName);
        }

        for (Entity nearby : armorStand.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, mainDamage, abilityName);
            }
        }


        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 40, 0.5, 0.5, 0.5, 0.2);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 30, 0.4, 0.4, 0.4, 0.1);

        for (Player p : impactLoc.getWorld().getPlayers()) {
            impactLoc.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1);
            impactLoc.getWorld().playSound(p.getLocation(), Sound.BLOCK_WOOD_BREAK, 1f, 1.5f);
        }


        spawnSlices(impactLoc);

        armorStand.remove();
    }

    private void spawnSlices(Location impactLoc) {
        Location spawnLoc = impactLoc.clone();

        for (int i = 0; i < 3; i++) {
            Item slice = spawnLoc.getWorld().dropItem(spawnLoc, new ItemStack(sliceMaterial));
            slice.setPickupDelay(Integer.MAX_VALUE);
            slice.setVelocity(new Vector(0, 0.5, 0));

            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.8f, 0.8f);

            new TrackedLauncherSliceTask(slice, shooter, sliceDamage, abilityName, teamManager, combatService)
                    .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        }
    }
}