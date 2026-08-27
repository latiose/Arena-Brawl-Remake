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

public class TrackedMelonTask extends BukkitRunnable {

    private final ArmorStand melonStand;
    private final Player shooter;
    private final double mainDamage;
    private final double sliceDamage;
    private final double aoeRadius;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private Vector velocity;
    private int ticksLived = 0;

    public TrackedMelonTask(ArmorStand melonStand, Vector velocity, Player shooter, double mainDamage, double sliceDamage,
                            double aoeRadius, String abilityName, TeamManager teamManager, CombatService combatService) {
        this.melonStand = melonStand;
        this.velocity = velocity;
        this.shooter = shooter;
        this.mainDamage = mainDamage;
        this.sliceDamage = sliceDamage;
        this.aoeRadius = aoeRadius;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public void run() {
        ticksLived++;

        if (!melonStand.isValid() || ticksLived > 100) {
            cancel();
            melonStand.remove();
            return;
        }

        velocity.setY(velocity.getY() - 0.04);
        Location nextLoc = melonStand.getLocation().add(velocity);
        melonStand.teleport(nextLoc);

        Location melonCenter = nextLoc.clone().add(0, 1.4, 0);

        for (Player candidate : melonStand.getWorld().getPlayers()) {
            if (candidate.equals(shooter) || !teamManager.isEnemy(shooter, candidate)) continue;

            if (candidate.getBoundingBox().expand(0.5).contains(melonCenter.toVector())) {
                explode(candidate, melonCenter);
                return;
            }
        }

        if (melonCenter.getBlock().getType().isSolid() && !melonCenter.getBlock().isPassable()) {
            explode(null, melonCenter);
        }
    }

    private void explode(Player directHitVictim, Location impactLoc) {
        cancel();

        if (directHitVictim != null) {
            combatService.applyAbilityDamage(shooter, directHitVictim, mainDamage, abilityName);
        }

        for (Entity nearby : melonStand.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, mainDamage, abilityName);
            }
        }

        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 40, 0.5, 0.5, 0.5, 0.2);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 30, 0.4, 0.4, 0.4, 0.1);

        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1);
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_WOOD_BREAK, 1f, 1.5f);

        spawnSlices(impactLoc);

        melonStand.remove();
    }

    private void spawnSlices(Location impactLoc) {
        Location spawnLoc = impactLoc.clone();

        for (int i = 0; i < 3; i++) {
            Item slice = spawnLoc.getWorld().dropItem(spawnLoc, new ItemStack(Material.MELON_SLICE));
            slice.setPickupDelay(Integer.MAX_VALUE);

            slice.setVelocity(new Vector(0, 0.5, 0));

            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.8f, 0.8f);

            new TrackedMelonSliceTask(slice, shooter, sliceDamage, abilityName, teamManager, combatService)
                    .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        }
    }
}