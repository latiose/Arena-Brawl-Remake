package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.AbilityTargeting;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Paranoia implements Ability, Listener {

    private static final double DEFAULT_REACH = 3.0;

    private final Plugin plugin;
    private final NamespacedKey reachKey;

    private final long blindnessDurationMs;
    private final long primeWindowMs;
    private final double damage;
    private final int dashTicks;
    private final double stopDistance;
    private final double maxRange;
    private final long chargeTimeMillis;

    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final MatchManager matchManager;

    private long primedUntil;
    private UUID primedOwner;
    private BukkitTask expiryTask;

    public Paranoia(Plugin plugin, CooldownManager cooldownManager, UsageManager usageManager,
                    TeamManager teamManager, CombatService combatService, DebuffManager debuffManager,
                    MatchManager matchManager, AbilityConfig config) {
        this.plugin = plugin;
        this.reachKey = new NamespacedKey(plugin, "paranoia_reach");

        this.blindnessDurationMs = config.getLong("blindness-duration-ms", 4000L);
        this.primeWindowMs = config.getLong("prime-window-ms", 6000L);
        this.damage = config.getDouble("damage", 300.0);
        this.dashTicks = Math.max(1, config.getInt("dash-ticks", 5));
        this.stopDistance = config.getDouble("stop-distance", 1.2);
        this.maxRange = config.getDouble("max-range", 256.0);
        this.chargeTimeMillis = config.getLong("charge-time-millis", 60000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "paranoia");
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.matchManager = matchManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getName() {
        return "Paranoia";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "paranoia", chargeTimeMillis);
    }

    private boolean isPrimed() {
        return System.currentTimeMillis() < primedUntil;
    }

    @Override
    public boolean activate(Player player) {
        Match match = matchManager.getMatchFor(player);
        if (match == null) return false;

        List<Player> enemies = new ArrayList<>();
        for (Player candidate : match.getAlivePlayers()) {
            if (candidate.equals(player)) continue;
            if (candidate.isDead()) continue;
            if (candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, candidate)) continue;
            enemies.add(candidate);
        }

        if (enemies.isEmpty()) return false;

        for (Player enemy : enemies) {
            debuffManager.tryApply(player, enemy, DebuffType.BLINDNESS, blindnessDurationMs);
            enemy.getWorld().spawnParticle(Particle.SQUID_INK,
                    enemy.getLocation().add(0, 1.0, 0), 20, 0.4, 0.6, 0.4, 0.03);
            enemy.getWorld().spawnParticle(Particle.DUST,
                    enemy.getLocation().add(0, 1.0, 0), 12, 0.35, 0.5, 0.35, 0.0,
                    new Particle.DustOptions(Color.fromRGB(45, 0, 60), 1.2f));
            enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_ENDERMAN_STARE, 0.8f, 0.5f);
        }

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 0.5f);
        player.getWorld().spawnParticle(Particle.SQUID_INK,
                player.getLocation().add(0, 1.0, 0), 30, 0.6, 0.8, 0.6, 0.05);

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
        double bonus = Math.max(0.0, maxRange - DEFAULT_REACH);
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
        if (target.isDead() || target.getGameMode() == GameMode.SPECTATOR) return;
        if (!teamManager.isEnemy(player, target)) return;

        event.setCancelled(true);
        unprime(player);
        dashToTarget(player, target);
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

    private void dashToTarget(Player player, Player target) {
        final Location start = player.getLocation().clone();
        final World world = start.getWorld();

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()
                        || player.getGameMode() == GameMode.SPECTATOR
                        || !target.isOnline() || target.isDead()
                        || target.getGameMode() == GameMode.SPECTATOR
                        || !target.getWorld().equals(world)) {
                    cancel();
                    return;
                }

                tick++;

                if (tick >= dashTicks) {
                    Location landing = resolveLanding(start, target);
                    player.teleport(landing);
                    player.setFallDistance(0);
                    impact(player, target, landing);
                    cancel();
                    return;
                }

                double progress = (double) tick / dashTicks;
                Location point = start.clone().add(
                        target.getLocation().toVector().subtract(start.toVector()).multiply(progress));
                point.setDirection(start.getDirection());
                player.teleport(point);
                player.setFallDistance(0);
                drawTrail(point);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private Location resolveLanding(Location start, Player target) {
        Location targetLoc = target.getLocation().clone();

        Vector back = start.toVector().subtract(targetLoc.toVector());
        back.setY(0);

        Location landing;
        if (back.length() > stopDistance + 0.15) {
            back.normalize().multiply(stopDistance);
            landing = targetLoc.clone().add(back);
            landing.setY(targetLoc.getY());
            if (isBlocked(landing)) {
                landing = AbilityTargeting.findBehindLocation(target);
            }
        } else {
            landing = AbilityTargeting.findBehindLocation(target);
        }

        landing.setDirection(start.getDirection());
        return landing;
    }

    private void drawTrail(Location point) {
        Location center = point.clone().add(0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.DUST, center, 6, 0.25, 0.35, 0.25, 0.0,
                new Particle.DustOptions(Color.fromRGB(45, 0, 60), 1.3f));
        center.getWorld().spawnParticle(Particle.SQUID_INK, center, 2, 0.2, 0.3, 0.2, 0.01);
    }

    private void impact(Player caster, Player target, Location landing) {
        World world = landing.getWorld();
        Location center = target.getLocation().add(0, 1.0, 0);

        world.spawnParticle(Particle.SWEEP_ATTACK, center, 4, 0.25, 0.35, 0.25, 0.0);
        world.spawnParticle(Particle.SQUID_INK, center, 30, 0.35, 0.6, 0.35, 0.05);
        world.spawnParticle(Particle.DUST, center, 25, 0.3, 0.5, 0.3, 0.0,
                new Particle.DustOptions(Color.fromRGB(45, 0, 60), 1.4f));
        world.playSound(landing, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        world.playSound(landing, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.9f);

        combatService.applyAbilityDamage(caster, target, damage, getName(), target.getLocation());
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }

    @Override
    public String getDescription() {
        return "Blinds every living enemy for " + (blindnessDurationMs / 1000L)
                + "s. For " + (primeWindowMs / 1000L)
                + "s your reach becomes unlimited and your next attack dashes to the enemy, dealing damage.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Blindness", (blindnessDurationMs / 1000L) + "s"),
                new AbilityStat("Window", (primeWindowMs / 1000L) + "s"),
                new AbilityStat("Range", "Global"),
                new AbilityStat("Uses", "1 per match")
        );
    }
}
