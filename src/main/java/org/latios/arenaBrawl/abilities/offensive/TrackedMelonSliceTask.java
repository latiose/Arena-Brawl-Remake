package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class TrackedMelonSliceTask extends BukkitRunnable {

    private final Item slice;
    private final Player shooter;
    private final double damage;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private int ticksLived = 0;
    private static final double SLICE_AOE_RADIUS = 3;

    public TrackedMelonSliceTask(Item slice, Player shooter, double damage, String abilityName,
                                 TeamManager teamManager, CombatService combatService) {
        this.slice = slice;
        this.shooter = shooter;
        this.damage = damage;
        this.abilityName = abilityName;
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public void run() {
        ticksLived++;

        if (!slice.isValid() || ticksLived > 80) {
            cancel();
            slice.remove();
            return;
        }

        Location sliceLoc = slice.getLocation();

        if (slice.isOnGround()) {
            explodeSlice(sliceLoc);
            return;
        }
        if (ticksLived < 20) return;

        List<Player> hitEnemies = new ArrayList<>();
        for (Entity entity : slice.getNearbyEntities(SLICE_AOE_RADIUS, SLICE_AOE_RADIUS, SLICE_AOE_RADIUS)) {
            if (entity instanceof Player candidate && !candidate.equals(shooter) && teamManager.isEnemy(shooter, candidate)) {
                hitEnemies.add(candidate);
            }
        }

        if (!hitEnemies.isEmpty()) {
            explodeSlice(sliceLoc);
        }
    }

    private void explodeSlice(Location loc) {
        cancel();

        for (Entity nearby : slice.getNearbyEntities(SLICE_AOE_RADIUS, SLICE_AOE_RADIUS, SLICE_AOE_RADIUS)) {
            if (nearby instanceof Player target && !target.equals(shooter) && teamManager.isEnemy(shooter, target)) {
                combatService.applyAbilityDamage(shooter, target, damage, abilityName);
            }
        }

        loc.getWorld().spawnParticle(Particle.ITEM_SLIME, loc, 12, 0.3, 0.3, 0.3, 0.1);
        loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_BREAK, 0.6f, 1.2f);

        slice.remove();
    }
}