package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.DamageVulnerabilityManager;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

/**
 * Offensive hitscan dagger. Deals bonus damage to any enemy that is currently affected by a
 * debuff or marked by a vulnerability (for example a teammate's Discord Orb or Polymorph),
 * which rewards coordinated focus fire in 2v2.
 */
public class Backstabber implements Ability {

    private final double damage;
    private final double bonusDamage;
    private final double energyCost;
    private final double maxRange;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final DamageVulnerabilityManager damageVulnerabilityManager;
    private final AbilityConfig config;

    public Backstabber(EnergyManager energyManager, TeamManager teamManager, CombatService combatService,
                       DebuffManager debuffManager, DamageVulnerabilityManager damageVulnerabilityManager,
                       AbilityConfig config) {
        this.damage = config.getDouble("damage", 90.0);
        this.bonusDamage = config.getDouble("bonus-damage", 80.0);
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.maxRange = config.getDouble("max-range", 20.0);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.damageVulnerabilityManager = damageVulnerabilityManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Backstabber";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Throws a spectral dagger in a line. It deals bonus damage to enemies that are "
                + "currently debuffed or marked.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", (int) damage + ""),
                new AbilityStat("Bonus Damage", "+" + (int) bonusDamage + " (debuffed/marked)"),
                new AbilityStat("Energy Cost", (int) energyCost + ""),
                new AbilityStat("Range", (int) maxRange + "m")
        );
    }

    @Override
    public boolean activate(Player player) {
        Player target = AbilityTargeting.findEnemyAlongRay(player, teamManager, maxRange);

        if (target == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        drawBeam(player.getEyeLocation().subtract(0, 0.2, 0), target.getEyeLocation());

        boolean exposed = debuffManager.hasActiveDebuff(target) || damageVulnerabilityManager.isActive(target);
        double totalDamage = exposed ? damage + bonusDamage : damage;

        Location impact = target.getLocation().add(0, 1.0, 0);
        combatService.applyAbilityDamage(player, target, totalDamage, getName(), impact);

        target.getWorld().spawnParticle(Particle.CRIT, impact, exposed ? 25 : 10, 0.3, 0.5, 0.3, 0.1);
        if (exposed) {
            target.getWorld().spawnParticle(Particle.ENCHANTED_HIT, impact, 15, 0.3, 0.5, 0.3, 0.1);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10.0f, 1.6f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 10.0f, 1.5f);

        return true;
    }

    private void drawBeam(Location start, Location end) {
        Vector direction = end.toVector().subtract(start.toVector());
        double length = direction.length();
        direction.normalize();

        for (double d = 0; d < length; d += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(d));
            point.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
            point.getWorld().spawnParticle(Particle.SMOKE, point, 1, 0, 0, 0, 0);
        }
    }
}
