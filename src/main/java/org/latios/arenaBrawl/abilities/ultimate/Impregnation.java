package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class Impregnation implements Ability {

    private final long chargeTimeMillis;
    private final double range;
    private final long gestationMillis;
    private final double babyHealth;
    private final int babyHitsToKill;
    private final double babyDamage;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final MatchManager matchManager;
    private final ZombieEntityManager entityManager;

    public Impregnation(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                        TeamManager teamManager, MatchManager matchManager,
                        ZombieEntityManager entityManager, AbilityConfig config) {
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);
        this.range = config.getDouble("max-range", 20.0);
        this.gestationMillis = config.getLong("gestation-millis", 20000L);
        this.babyHealth = config.getDouble("baby-health", 500.0);
        this.babyHitsToKill = config.getInt("baby-hits-to-kill", 12);
        this.babyDamage = config.getDouble("baby-damage", 60.0);

        this.plugin = plugin;
        this.cooldownManager = cooldownManager;
        this.cost = new UltimateCost(cooldownManager, usageManager, "impregnation");
        this.teamManager = teamManager;
        this.matchManager = matchManager;
        this.entityManager = entityManager;
    }

    @Override
    public String getName() {
        return "Impregnation";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "impregnation", chargeTimeMillis);
    }

    @Override
    public boolean activate(Player player) {
        Player ally = AbilityTargeting.findAllyAlongRay(player, teamManager, range);

        if (ally == null) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        startPregnancy(player, ally);

        ally.sendMessage("§a" + player.getName() + " §eimpregnated you! You will give birth in "
                + (gestationMillis / 1000L) + "s if you survive.");

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, 1.0f, 1.2f);
        ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 1.0, 0),
                12, 0.5, 0.6, 0.5, 0.02);

        return true;
    }

    private void startPregnancy(Player caster, Player mother) {
        long totalTicks = Math.max(20L, gestationMillis / 50L);

        new BukkitRunnable() {
            long elapsedTicks = 0L;

            @Override
            public void run() {
                if (!isValidMother(mother)) {
                    cancel();
                    return;
                }

                elapsedTicks += 20L;

                if (elapsedTicks >= totalTicks) {
                    cancel();
                    giveBirth(caster, mother);
                    return;
                }

                drawPregnancyParticles(mother);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private boolean isValidMother(Player mother) {
        Match match = matchManager.getMatchFor(mother);
        return mother.isOnline()
                && !mother.isDead()
                && mother.getGameMode() != GameMode.SPECTATOR
                && match != null
                && !match.isEnded();
    }

    private void drawPregnancyParticles(Player mother) {
        mother.getWorld().spawnParticle(Particle.HEART,
                mother.getLocation().add(0, 1.0, 0),
                3, 0.35, 0.5, 0.35, 0.0);
    }

    private void giveBirth(Player caster, Player mother) {
        Location birthLocation = mother.getLocation();
        World world = birthLocation.getWorld();

        entityManager.summonBabyZombie(mother, birthLocation, babyHealth, babyHitsToKill, babyDamage);

        world.spawnParticle(Particle.HEART, birthLocation.clone().add(0, 1.0, 0),
                30, 0.7, 0.9, 0.7, 0.05);
        world.spawnParticle(Particle.EXPLOSION, birthLocation, 1);
        world.playSound(birthLocation, Sound.ENTITY_ZOMBIE_AMBIENT, 1.6f, 0.6f);
        world.playSound(birthLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);

    }

    @Override
    public String getDescription() {
        return "Impregnates the targeted ally. After " + (gestationMillis / 1000L)
                + "s, if they are still alive, they give birth to a powerful baby zombie that hunts your enemies.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Gestation", (gestationMillis / 1000L) + "s"),
                new AbilityStat("Range", (int) range + " blocks"),
                new AbilityStat("Baby Health", String.valueOf((int) babyHealth)),
                new AbilityStat("Baby Hits to Kill", String.valueOf(babyHitsToKill)),
                new AbilityStat("Baby Damage", String.valueOf((int) babyDamage))
        );
    }
}
