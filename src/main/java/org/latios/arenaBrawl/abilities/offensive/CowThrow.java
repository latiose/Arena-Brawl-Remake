package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class CowThrow implements Ability {

    private final AbilityCost cost;
    private final double damage;
    private final double speed;
    private final double explosionRadius;
    private final double energyCost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final Plugin plugin;

    public CowThrow(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                    CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.damage = config.getDouble("damage", 150.0);
        this.speed = config.getDouble("speed", 1.8);
        this.explosionRadius = config.getDouble("explosion-radius", 4.0);
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Cow Throw"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.2));
        Vector direction = player.getLocation().getDirection().normalize().multiply(speed);

        player.getWorld().playSound(spawnLoc, Sound.ENTITY_GHAST_SHOOT, 1.2f, 0.5f);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_COW_HURT, 1.5f, 0.6f);

        Cow cow = (Cow) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.COW);
        cow.setNoDamageTicks(Integer.MAX_VALUE);
        cow.setInvulnerable(true);
        cow.setLootTable(null);
        cow.setVelocity(direction);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                if (cow.isDead() || !cow.isValid() || ticks > 100 || cow.isOnGround() || isHittingEnemy(cow, player)) {
                    explodeCow(cow, player);
                    cancel();
                    return;
                }

                Location cowLoc = cow.getLocation();
               // cowLoc.getWorld().spawnParticle(Particle.EXPLOSION, cowLoc, 1, 0.1, 0.1, 0.1, 0.0);
                cowLoc.getWorld().spawnParticle(Particle.CLOUD, cowLoc, 3, 0.2, 0.2, 0.2, 0.05);
            }
        }.runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    private boolean isHittingEnemy(Cow cow, Player caster) {
        for (Entity entity : cow.getNearbyEntities(1.2, 1.2, 1.2)) {
            if (entity instanceof Player target) {
                if (!target.equals(caster) && teamManager.isEnemy(caster, target) && !target.isDead()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void explodeCow(Cow cow, Player caster) {
        Location impactLoc = cow.getLocation();

        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, impactLoc, 1);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_COW_DEATH, 1.5f, 0.5f);

        for (Player target : impactLoc.getWorld().getPlayers()) {
            if (target.equals(caster) || !teamManager.isEnemy(caster, target) || target.isDead()) {
                continue;
            }

            double distance = target.getLocation().distance(impactLoc);
            if (distance <= explosionRadius) {
                combatService.applyAbilityDamage(caster, target, damage, getName(), impactLoc);
            }
        }

        cow.remove();
    }

    @Override
    public String getDescription() {
        return "Hurl a massive cow forward with superhuman strength. Explodes on contact, dealing heavy area damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Radius", (int) explosionRadius + "m"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}