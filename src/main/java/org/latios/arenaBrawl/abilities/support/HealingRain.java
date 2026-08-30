package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class HealingRain implements Ability {

    private static final long COOLDOWN_MS = 30_000;
    private static final double HEAL_PER_SECOND = 50.0;
    private static final double RADIUS = 4;
    private static final int DURATION_SECONDS = 6;
    private static final double CLOUD_HEIGHT = 3.5;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public HealingRain(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                       PlayerHealthManager healthManager, CombatUpgradeManager combatUpgradeManager) {
        this.plugin = plugin;
        this.cost = new CooldownCost(cooldownManager, "healingrain", COOLDOWN_MS, combatUpgradeManager);
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public String getName() {
        return "Healing Rain";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location groundCenter = player.getLocation().getBlock().getLocation().add(0.5, 0.1, 0.5);
        Location cloudCenter = groundCenter.clone().add(0, CLOUD_HEIGHT, 0);

        groundCenter.getWorld().playSound(groundCenter, Sound.WEATHER_RAIN, 1.0f, 1.2f);
        groundCenter.getWorld().playSound(groundCenter, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f);

        new BukkitRunnable() {
            private int ticksRun = 0;
            private final int totalTicks = DURATION_SECONDS * 20;

            @Override
            public void run() {
                ticksRun += 2;

                if (ticksRun >= totalTicks) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 6; i++) {
                    double offsetX = (Math.random() - 0.5) * RADIUS * 1.2;
                    double offsetZ = (Math.random() - 0.5) * RADIUS * 1.2;
                    double offsetY = (Math.random() - 0.5) * 0.4;
                    cloudCenter.getWorld().spawnParticle(Particle.CLOUD, cloudCenter.clone().add(offsetX, offsetY, offsetZ), 1, 0, 0, 0, 0);
                }

                for (int i = 0; i < 4; i++) {
                    double dropX = (Math.random() - 0.5) * RADIUS * 1.8;
                    double dropZ = (Math.random() - 0.5) * RADIUS * 1.8;
                    Location rainStart = cloudCenter.clone().add(dropX, 0, dropZ);
                    rainStart.getWorld().spawnParticle(Particle.FALLING_WATER, rainStart, 2, 0.1, 0.5, 0.1, 0);
                }

                if (ticksRun % 20 == 0) {
                    groundCenter.getWorld().playSound(groundCenter, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 0.4f, 1.8f);

                    for (Player ally : groundCenter.getWorld().getPlayers()) {
                        if (ally.isDead() || teamManager.isEnemy(player, ally)) continue;

                        Location allyLoc = ally.getLocation();
                        double distance2D = Math.hypot(allyLoc.getX() - groundCenter.getX(), allyLoc.getZ() - groundCenter.getZ());
                        boolean inVerticalRange = allyLoc.getY() >= groundCenter.getY() - 1.0 && allyLoc.getY() <= cloudCenter.getY();

                        if (distance2D <= RADIUS && inVerticalRange) {
                            healthManager.heal(ally, HEAL_PER_SECOND);
                            ally.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, ally.getLocation().add(0, 1.0, 0), 5, 0.2, 0.4, 0.2, 0);

                            if (ally.equals(player)) {
                                player.sendMessage(MessageUtils.positive() + String.format("§3Your Healing Rain healed you for §a%d §3health!", (int) HEAL_PER_SECOND));
                            } else {
                                player.sendMessage(MessageUtils.positive() + String.format("§3Your Healing Rain healed §a%s §3for §a%d §3health!",
                                        ally.getName(), (int) HEAL_PER_SECOND));
                                ally.sendMessage(MessageUtils.positive() + String.format("§a%s§3's Healing Rain healed you for §a%d §3health!",
                                        player.getName(), (int) HEAL_PER_SECOND));
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a stationary rain cloud that heals you and nearby allies inside it over 6 seconds.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Heal/sec", (int) HEAL_PER_SECOND + " HP"),
                new AbilityStat("Duration", DURATION_SECONDS + "s"),
                new AbilityStat("Radius", RADIUS + "m"),
                new AbilityStat("Cooldown", (COOLDOWN_MS / 1000) + "s")
        );
    }
}