package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Teleport implements Ability {

    private static final double STEP_SIZE = 0.25;

    private final double maxDistance;
    private final double aoeHalfSize;
    private final double damage;
    private final double energyCost;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    public Teleport(EnergyManager energyManager, TeamManager teamManager,
                    CombatService combatService, AbilityConfig config) {
        this.maxDistance = config.getDouble("max-distance", 8.0);
        // Half size of a 3x3 grid (1.5) so every block in a 3x3 around the arrival
        // point is covered.
        this.aoeHalfSize = config.getDouble("aoe-half-size", 1.5);
        this.damage = config.getDouble("damage", 170.0);
        this.energyCost = config.getDouble("energy-cost", 80.0);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Teleport";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Instantly teleports a short distance in the direction you are looking and deals "
                + (int) damage + " damage to enemies an area around your arrival point.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Distance", (int) maxDistance + " blocks"),
                new AbilityStat("Area", "3x3"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation();
        Vector direction = player.getEyeLocation().getDirection();
        direction.normalize();

        // Destination is the maximum range along the look direction. If that
        // point is blocked, walk backwards from it until a free position is
        // found, so the teleport always happens and always lands as far away
        // as the look direction allows.
        Location lastSafe = resolveDestination(player, direction);

        lastSafe.setYaw(origin.getYaw());
        lastSafe.setPitch(origin.getPitch());

        origin.getWorld().spawnParticle(Particle.PORTAL, origin.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.2);
        player.teleport(lastSafe);
        player.setFallDistance(0);
        // The actual arrival point: the vanilla teleport can adjust the
        // destination, so damage is computed around where the player really
        // ended up.
        Location arrival = player.getLocation();

        damageAround(player, arrival);

        arrival.getWorld().spawnParticle(Particle.PORTAL, arrival.clone().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.2);
        arrival.getWorld().spawnParticle(Particle.END_ROD, arrival.clone().add(0, 1.2, 0), 20, 0.5, 0.4, 0.5, 0.05);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.6f);

        return true;
    }

    private void damageAround(Player caster, Location center) {
        boolean hitAny = false;

        for (Player target : center.getWorld().getPlayers()) {
            if (target.equals(caster) || target.isDead()
                    || target.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(caster, target)) continue;

            Location loc = target.getLocation();
            if (Math.abs(loc.getX() - center.getX()) > aoeHalfSize) continue;
            if (Math.abs(loc.getZ() - center.getZ()) > aoeHalfSize) continue;
            if (loc.getY() < center.getY() - 2.0 || loc.getY() > center.getY() + 2.0) continue;

            combatService.applyAbilityDamage(caster, target, damage, getName(), loc);
            hitAny = true;
        }

        if (hitAny) {
            caster.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 0.9f);
        }
    }

    private Location resolveDestination(Player player, Vector direction) {
        Location eye = player.getEyeLocation();

        for (double offset = 0.0; offset <= maxDistance; offset += STEP_SIZE) {
            Location candidate = eye.clone()
                    .add(direction.clone().multiply(maxDistance - offset));

            if (!isBlocked(candidate) && isInWorld(candidate)) {
                return candidate;
            }
        }

        return eye.clone().add(direction.clone().multiply(STEP_SIZE));
    }

    private boolean isBlocked(Location destination) {
        return destination.getBlock().getType().isSolid()
                || destination.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    private boolean isInWorld(Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        int y = location.getBlockY();
        return y >= location.getWorld().getMinHeight() && y < location.getWorld().getMaxHeight() - 1;
    }
}
