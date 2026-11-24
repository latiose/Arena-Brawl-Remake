package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilitySlot;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.UUID;

public class PhaseDive implements Ability, Listener {

    private static final double STEP_SIZE = 0.2;
    private static final double DEFAULT_REACH = 3.0;

    private final Plugin plugin;
    private final NamespacedKey reachKey;

    private final double energyCost;
    private final double damage;
    private final double firstDashDistance;
    private final double targetRange;
    private final long primeWindowMs;
    private final double stopDistance;


    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;

    private long primedUntil;
    private UUID primedOwner;
    private BukkitTask expiryTask;
    private boolean freeCastPending;

    public PhaseDive(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                     CombatService combatService, AbilityConfig config) {
        this.plugin = plugin;
        this.reachKey = new NamespacedKey(plugin, "phase_dive_reach");

        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.damage = config.getDouble("damage", 150.0);
        this.firstDashDistance = config.getDouble("first-dash-distance", 3.5);
        this.targetRange = config.getDouble("target-range", 12.0);
        this.primeWindowMs = config.getLong("prime-window-ms", 4000L);
        this.stopDistance = config.getDouble("stop-distance", 1.2);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @Override
    public String getName() {
        return "Phase Dive";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    private boolean isPrimed() {
        return System.currentTimeMillis() < primedUntil;
    }


    @Override
    public boolean activate(Player player) {
        firstDash(player);
        prime(player);
        return true;
    }

    private void prime(Player player) {
        primedUntil = System.currentTimeMillis() + primeWindowMs;
        primedOwner = player.getUniqueId();
        applyReach(player);

        if (expiryTask != null) {
            expiryTask.cancel();
        }
        expiryTask = plugin.getServer().getScheduler().runTaskLater(
                plugin, () -> unprime(player), (primeWindowMs / 50L) + 1L);
    }

    private void unprime(Player player) {
        primedUntil = 0;
        primedOwner = null;
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
        removeReach(player);
    }

    private void applyReach(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attribute == null) return;

        attribute.removeModifier(reachKey);
        double bonus = Math.max(0.0, targetRange - DEFAULT_REACH);
        attribute.addTransientModifier(
                new AttributeModifier(reachKey, bonus, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeReach(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE);
        if (attribute != null) {
            attribute.removeModifier(reachKey);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!isPrimed() || !player.getUniqueId().equals(primedOwner)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        if (target.isDead() || !teamManager.isEnemy(player, target)) return;
        if (!player.hasLineOfSight(target)) return;

        event.setCancelled(true);
        diveToTarget(player, target);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(primedOwner)) {
            unprime(event.getPlayer());
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().getUniqueId().equals(primedOwner)) {
            unprime(event.getEntity());
        }
    }

    private void firstDash(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0);
        if (direction.lengthSquared() < 1e-6) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();

        Location start = player.getLocation().clone();
        Location lastSafe = start.clone();
        double traveled = 0;

        while (traveled < firstDashDistance) {
            Location next = lastSafe.clone().add(direction.clone().multiply(STEP_SIZE));
            if (isBlocked(next)) break;
            lastSafe = next;
            traveled += STEP_SIZE;
        }

        player.teleport(lastSafe);
        drawDash(start, lastSafe);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.4f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
    }

    private void diveToTarget(Player player, Player target) {
        unprime(player);

        Location start = player.getLocation().clone();
        Location targetLoc = target.getLocation().clone();

        Vector back = start.toVector().subtract(targetLoc.toVector());
        back.setY(0);

        Location landing;
        double horizontal = back.length();
        if (horizontal > stopDistance + 0.15) {
            back.normalize().multiply(stopDistance);
            landing = targetLoc.clone().add(back);
            landing.setY(targetLoc.getY());
            if (isBlocked(landing)) {
                landing = AbilityTargeting.findBehindLocation(target);
            }
        } else {
            landing = start.clone();
        }
        landing.setDirection(start.getDirection());

        player.teleport(landing);
        drawDash(start, landing);

        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1.0, 0),
                3, 0.2, 0.3, 0.2, 0.0);
        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1.0, 0),
                15, 0.3, 0.4, 0.3, 0.04);
        player.getWorld().playSound(landing, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
        player.getWorld().playSound(landing, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 1.2f);

        combatService.applyAbilityDamage(player, target, damage, getName(), targetLoc);
    }

    private void drawDash(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length <= 0) return;

        direction.normalize().multiply(0.35);
        Location point = from.clone().add(0, 1.0, 0);
        double traveled = 0;

        while (traveled < length) {
            point.getWorld().spawnParticle(Particle.END_ROD, point, 1, 0.02, 0.02, 0.02, 0.01);
            point.getWorld().spawnParticle(Particle.DUST, point, 2, 0.05, 0.05, 0.05,
                    new Particle.DustOptions(Color.fromRGB(90, 255, 210), 1.0f));
            point.add(direction);
            traveled += 0.35;
        }
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    @Override
    public String getDescription() {
        return "Dashes a short distance. For " + (primeWindowMs / 1000L)
                + "s your reach is extended and your next attack dives to the enemy, dealing damage. "
                + "Recasting dashes again without damage and refreshes the timer.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("First Dash", String.format("%.1f blocks", firstDashDistance)),
                new AbilityStat("Dive Range", (int) targetRange + " blocks"),
                new AbilityStat("Window", (primeWindowMs / 1000L) + "s"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}