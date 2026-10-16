package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SeekerBolt implements Ability {

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final AbilityConfig config;

    private final double energyCost;
    private final int boltCount;
    private final long boltIntervalTicks;
    private final double damage;
    private final double aoeRadius;
    private final double speed;
    private final double turnRate;
    private final double trackRadius;
    private final double hitRadius;
    private final double spread;
    private final int maxLifetimeTicks;

    public SeekerBolt(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                      CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.boltCount = Math.max(1, config.getInt("bolt-count", 3));
        this.boltIntervalTicks = config.getLong("bolt-interval-ticks", 4L);
        this.damage = config.getDouble("damage", 70.0);
        this.aoeRadius = config.getDouble("aoe-radius", 1.5);
        this.speed = config.getDouble("speed", 0.7);
        this.turnRate = config.getDouble("turn-rate", 0.25);
        this.trackRadius = config.getDouble("track-radius", 25.0);
        this.hitRadius = config.getDouble("hit-radius", 0.6);
        this.spread = config.getDouble("spread", 0.12);
        this.maxLifetimeTicks = config.getInt("max-lifetime-ticks", 90);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.config = config;
    }

    @Override
    public String getName() { return "Seeker Bolt"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        MatchSoundUtils.play(config, player, Sound.ENTITY_ENDER_EYE_LAUNCH, 0.9f, 1.2f);
        MatchSoundUtils.play(config, player, Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.6f);

        for (int i = 0; i < boltCount; i++) {
            final long delay = i * boltIntervalTicks;
            Bukkit.getScheduler().runTaskLater(plugin, () -> launchBolt(player), delay);
        }
        return true;
    }

    private void launchBolt(Player player) {
        if (!player.isOnline() || player.isDead()) return;

        Snowball bolt = player.launchProjectile(Snowball.class);
        Vector direction = player.getEyeLocation().getDirection().normalize();

        if (spread > 0) {
            direction.add(new Vector(
                    ThreadLocalRandom.current().nextDouble(-spread, spread),
                    ThreadLocalRandom.current().nextDouble(-spread, spread),
                    ThreadLocalRandom.current().nextDouble(-spread, spread))).normalize();
        }

        bolt.setGravity(false);
        bolt.setVelocity(direction.multiply(speed));

        new SeekerBoltTask(bolt, player, damage, getName(), speed, turnRate, trackRadius,
                hitRadius, aoeRadius, maxLifetimeTicks, teamManager, combatService)
                .runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getDescription() {
        return "Fires " + boltCount + " slow homing bolts that seek out the nearest enemy, dealing damage on impact.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Bolts", String.valueOf(boltCount)),
                new AbilityStat("Damage per Bolt", String.valueOf((int) damage)),
                new AbilityStat("Tracking", (int) trackRadius + " blocks"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
