package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

public class TrackedMelonSliceTask extends BukkitRunnable {

    private final Item slice;
    private final Player shooter;
    private final double damage;
    private final String abilityName;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private int ticksLived = 0;

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

        if (slice.isOnGround()) {
            cancel();
            slice.remove();
            return;
        }

        if (ticksLived < 5) return;

        for (Player candidate : slice.getWorld().getPlayers()) {
            if (candidate.equals(shooter) || !teamManager.isEnemy(shooter, candidate)) continue;

            if (candidate.getBoundingBox().expand(0.4).contains(slice.getLocation().toVector())) {
                cancel();
                combatService.applyAbilityDamage(shooter, candidate, damage, abilityName);
                slice.getWorld().spawnParticle(Particle.ITEM_SLIME, slice.getLocation(), 5);
                slice.remove();
                return;
            }
        }
    }
    }
