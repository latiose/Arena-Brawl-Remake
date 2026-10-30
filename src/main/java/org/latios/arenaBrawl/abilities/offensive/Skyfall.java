package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Anti-air strike. Hits every enemy in a vertical cylinder above the caster and
 * deals bonus damage (plus a hard knockdown) to targets that are airborne or
 * fighting from higher ground.
 */
public class Skyfall implements Ability {

    private final double energyCost;
    private final double damage;
    private final double radius;
    private final double height;
    private final double airborneBonus;
    private final double knockDown;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public Skyfall(EnergyManager energyManager, TeamManager teamManager,
                   CombatService combatService, AbilityConfig config) {
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.damage = config.getDouble("damage", 120.0);
        this.radius = config.getDouble("radius", 3.0);
        this.height = config.getDouble("height", 10.0);
        this.airborneBonus = config.getDouble("airborne-bonus", 1.6);
        this.knockDown = config.getDouble("knock-down", 0.8);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() {
        return "Skyfall";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation();
        World world = player.getWorld();

        List<Player> targets = new ArrayList<>();
        for (Entity nearby : world.getNearbyEntities(origin, radius, height, radius)) {
            if (!(nearby instanceof Player target)) continue;
            if (target.equals(player) || target.isDead()) continue;
            if (target.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, target)) continue;

            double dy = target.getLocation().getY() - origin.getY();
            if (dy < 0.0 || dy > height) continue;
            targets.add(target);
        }

        if (targets.isEmpty()) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        drawBeam(world, origin);
        world.playSound(origin, Sound.ITEM_TRIDENT_THUNDER, 0.8f, 1.5f);
        world.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.7f, 1.4f);

        for (Player target : targets) {
            double dy = target.getLocation().getY() - origin.getY();
            boolean airborne = !target.isOnGround() || dy >= 2.0;
            double appliedDamage = airborne ? damage * airborneBonus : damage;

            combatService.applyAbilityDamage(
                    player, target, appliedDamage, getName(),
                    target.getLocation().add(0, 1.0, 0)
            );

            if (airborne) {
                Vector velocity = target.getVelocity();
                target.setVelocity(new Vector(
                        velocity.getX() * 0.4,
                        -knockDown,
                        velocity.getZ() * 0.4
                ));
                target.getWorld().spawnParticle(
                        Particle.CRIT, target.getLocation().add(0, 1.0, 0),
                        20, 0.3, 0.5, 0.3, 0.15
                );
            }
        }

        return true;
    }

    private void drawBeam(World world, Location origin) {
        for (double y = 0; y <= height; y += 0.4) {
            Location point = origin.clone().add(0, y, 0);
            world.spawnParticle(Particle.END_ROD, point, 1, 0.12, 0.05, 0.12, 0.0);
        }

        world.spawnParticle(
                Particle.SWEEP_ATTACK, origin.clone().add(0, 0.4, 0),
                8, radius, 0.2, radius, 0.0
        );
        world.spawnParticle(
                Particle.CRIT, origin.clone().add(0, height * 0.5, 0),
                35, radius * 0.5, height * 0.5, radius * 0.5, 0.15
        );
    }

    @Override
    public String getDescription() {
        return "Calls a vertical strike above you, hitting every enemy in a column. "
                + "Airborne targets and enemies on higher ground take extra damage and are knocked down.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + " HP"),
                new AbilityStat("Airborne bonus", "+" + (int) ((airborneBonus - 1.0) * 100) + "%"),
                new AbilityStat("Radius", (int) radius + " blocks"),
                new AbilityStat("Height", (int) height + " blocks"),
                new AbilityStat("Energy", (int) energyCost + "")
        );
    }
}
