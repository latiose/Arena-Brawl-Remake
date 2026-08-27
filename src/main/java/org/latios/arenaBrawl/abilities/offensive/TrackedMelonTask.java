package org.latios.arenaBrawl.general;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.offensive.TrackedMelonSliceTask;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.Random;

public class TrackedMelonTask extends BukkitRunnable {

    private final Item melon;
    private final Player shooter;
    private final double mainDamage;
    private final double sliceDamage;
    private final double aoeRadius;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Random random = new Random();
    private int ticksLived = 0;

    public TrackedMelonTask(Item melon, Player shooter, double mainDamage, double sliceDamage,
                            double aoeRadius, String abilityName, TeamManager teamManager, CombatService combatService) {
        this.melon = melon;
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

        if (!melon.isValid() || ticksLived > 100) {
            cancel();
            melon.remove();
            return;
        }

        Location loc = melon.getLocation();

        for (Player candidate : melon.getWorld().getPlayers()) {
            if (candidate.equals(shooter) || !teamManager.isEnemy(shooter, candidate)) continue;

            if (candidate.getBoundingBox().expand(0.6).contains(loc.toVector())) {
                explode(candidate);
                return;
            }
        }

        if (melon.isOnGround()) {
            explode(null);
        }
    }

    private void explode(Player directHitVictim) {
        cancel();
        Location impactLoc = melon.getLocation();

        if (directHitVictim != null) {
            combatService.applyAbilityDamage(shooter, directHitVictim, mainDamage, abilityName);
        }

        for (Entity nearby : melon.getNearbyEntities(aoeRadius, aoeRadius, aoeRadius)) {
            if (nearby instanceof Player target && !target.equals(directHitVictim)
                    && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, mainDamage, abilityName);
            }
        }

        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 40, 0.5, 0.5, 0.5, 0.2);
        impactLoc.getWorld().spawnParticle(Particle.ITEM_SLIME, impactLoc, 30, 0.4, 0.4, 0.4, 0.1);

        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);
        impactLoc.getWorld().playSound(impactLoc, Sound.BLOCK_WOOD_BREAK, 1.5f, 0.5f);

        spawnSlices(impactLoc);

        melon.remove();
    }

    private void spawnSlices(Location impactLoc) {
        Location spawnLoc = impactLoc.clone().add(0, 1.0, 0);

        for (int i = 0; i < 3; i++) {
            Item slice = spawnLoc.getWorld().dropItem(spawnLoc, new ItemStack(Material.MELON_SLICE));
            slice.setPickupDelay(Integer.MAX_VALUE);

            slice.setVelocity(new Vector(0, 1, 0));

            spawnLoc.getWorld().playSound(spawnLoc, Sound.BLOCK_SLIME_BLOCK_STEP, 0.8f, 0.8f);

            new TrackedMelonSliceTask(slice, shooter, sliceDamage, abilityName, teamManager, combatService)
                    .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);
        }
    }
}