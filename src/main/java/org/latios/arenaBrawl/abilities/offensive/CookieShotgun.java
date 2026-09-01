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
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CookieShotgun implements Ability {

    private final AbilityCost cost;
    private final double damagePerCookie;
    private final double energyCost;
    private final int cookieCount;
    private final double explosionRadius;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public CookieShotgun(EnergyManager energyManager, TeamManager teamManager, CombatService combatService, AbilityConfig config) {
        this.damagePerCookie = config.getDouble("damage-per-cookie", 25.0);
        this.energyCost = config.getDouble("energy-cost", 60.0);
        this.cookieCount = config.getInt("cookie-count", 7);
        this.explosionRadius = config.getDouble("explosion-radius", 1.8);
        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Cookie Shotgun"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Fires " + cookieCount + " explosive cookies in a shotgun spread pattern. Each cookie explodes upon impact, dealing area damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage / Cookie", String.valueOf((int) damagePerCookie)),
                new AbilityStat("Cookies Fired", String.valueOf(cookieCount)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost)),
                new AbilityStat("AoE Radius", explosionRadius + " blocks")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();

        player.getWorld().playSound(eyeLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.8f);
        player.getWorld().playSound(eyeLoc, Sound.ENTITY_ITEM_BREAK, 1.2f, 0.6f);

        for (int i = 0; i < cookieCount; i++) {
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
        for (Entity entity : impactLoc.getWorld().getNearbyEntities(impactLoc, explosionRadius, explosionRadius, explosionRadius)) {
            if (entity instanceof Player victim && !victim.equals(owner)) {
                if (teamManager.isEnemy(owner, victim)) {
                    targets.add(victim);
                }
            }
        }

        for (Player victim : targets) {
            combatService.applyAbilityDamage(owner, victim, damagePerCookie, getName(), impactLoc);
        }
    }

    private Vector applySpread(Vector original, double spread) {
        double x = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        double y = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        double z = (ThreadLocalRandom.current().nextDouble() - 0.5) * spread;
        return original.add(new Vector(x, y, z)).normalize();
    }
}