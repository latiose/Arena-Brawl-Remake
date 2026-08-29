package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
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
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CookieShotgunAbility implements Ability {

    private static final double DAMAGE_PER_COOKIE = 25.0;
    private static final double ENERGY_COST = 60.0;
    private static final int COOKIE_COUNT = 7;
    private static final double EXPLOSION_RADIUS = 1.8;

    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public CookieShotgunAbility(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Cookie Shotgun"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires 7 explosive cookies in a shotgun spread pattern. Each cookie explodes upon impact, dealing area damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage / Cookie", "25"),
                new AbilityStat("Cookies Fired", String.valueOf(COOKIE_COUNT)),
                new AbilityStat("Energy Cost", "60"),
                new AbilityStat("AoE Radius", EXPLOSION_RADIUS + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.8f);
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_ITEM_BREAK, 1.2f, 0.6f);

        for (int i = 0; i < COOKIE_COUNT; i++) {
            Vector spreadDir = applySpread(direction.clone(), 0.28);
            double speed = 1.3 + (ThreadLocalRandom.current().nextDouble() * 0.4);

            Item cookie = player.getWorld().dropItem(eyeLoc, new ItemStack(Material.COOKIE));
            cookie.setPickupDelay(Integer.MAX_VALUE);
            cookie.setVelocity(spreadDir.multiply(speed));

            new BukkitRunnable() {
                int ticks = 0;

                @Override
                public void run() {
                    ticks++;

                    if (!cookie.isValid() || cookie.isOnGround() || ticks > 40) {
                        cancel();
                        explodeCookie(player, cookie);
                        return;
                    }

                    cookie.getWorld().spawnParticle(
                            Particle.DUST,
                            cookie.getLocation(),
                            2,
                            0.05, 0.05, 0.05,
                            new Particle.DustOptions(Color.fromRGB(180, 120, 60), 0.8f)
                    );

                    for (Entity nearby : cookie.getNearbyEntities(0.6, 0.6, 0.6)) {
                        if (nearby instanceof Player victim && !victim.equals(player) && teamManager.isEnemy(player, victim)) {
                            cancel();
                            explodeCookie(player, cookie);
                            return;
                        }
                    }
                }
            }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 1L, 1L);
        }

        return true;
    }

    private void explodeCookie(Player owner, Item cookie) {
        Location impactLoc = cookie.getLocation();
        cookie.remove();

        impactLoc.getWorld().spawnParticle(
                Particle.ITEM,
                impactLoc,
                15,
                0.3, 0.3, 0.3,
                0.1,
                new ItemStack(Material.COOKIE)
        );
        impactLoc.getWorld().spawnParticle(Particle.EXPLOSION, impactLoc, 1, 0, 0, 0, 0);
        impactLoc.getWorld().playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 2.0f);

        Set<Player> targets = new HashSet<>();
        for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
            if (entity instanceof Player victim && !victim.equals(owner)) {
                if (teamManager.isEnemy(owner, victim)) {
                    targets.add(victim);
                }
            }
        }

        for (Player victim : targets) {
            combatService.applyAbilityDamage(owner, victim, DAMAGE_PER_COOKIE, getName(), impactLoc);
        }
    }

    private Vector applySpread(Vector original, double spread) {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        double y = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        return original.add(new Vector(x, y, z)).normalize();
    }
}