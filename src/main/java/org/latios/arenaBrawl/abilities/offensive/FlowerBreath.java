package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.EntityCleanupUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Breathes a cone of petals that damages enemies and plants a random flower
 * on the ground. The flower stays there until an enemy steps on it, applying
 * a colour-coded debuff before wilting.
 */
public class FlowerBreath extends Breath {

    private final double damage;
    private final double energyCost;
    private final double triggerRadius;
    private final long flowerLifetimeMs;
    private final long blueSlowDurationMs;
    private final long redFireDurationMs;
    private final double redFireTickDamage;
    private final long redFireIntervalMs;
    private final double yellowEnergyDrain;
    private final long witheredBlindDurationMs;
    private final EnergyManager energyManager;
    private final StructureManager structureManager;
    private final double feetSpawnChance;
    private int hitsThisCast;
    public FlowerBreath(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                        CombatService combatService, DebuffManager debuffManager,
                        StructureManager structureManager, AbilityConfig config) {
        super(plugin, energyManager, config.getDouble("energy-cost", 80.0), teamManager, combatService,
                debuffManager, config.getMatchManager());
        this.damage = config.getDouble("damage", 180.0);
        this.energyCost = config.getDouble("energy-cost", 80.0);
        this.triggerRadius = config.getDouble("trigger-radius", 1.3);
        this.flowerLifetimeMs = config.getLong("flower-lifetime-ms", 10_000L);
        this.blueSlowDurationMs = config.getLong("blue-slow-duration-ms", 2_000L);
        this.redFireDurationMs = config.getLong("red-fire-duration-ms", 2_000L);
        this.redFireTickDamage = config.getDouble("red-fire-tick-damage", 15.0);
        this.redFireIntervalMs = config.getLong("red-fire-interval-ms", 500L);
        this.yellowEnergyDrain = config.getDouble("yellow-energy-drain", 10.0);
        this.witheredBlindDurationMs = config.getLong("withered-blind-duration-ms", 1_000L);
        this.feetSpawnChance = config.getDouble("feet-spawn-chance", 0.75);
        this.energyManager = energyManager;
        this.structureManager = structureManager;
    }

    @Override
    public String getName() {
        return "Flower Breath";
    }

    @Override
    protected Sound getCastSound() {
        return Sound.BLOCK_GRASS_BREAK;
    }

    @Override
    protected double getDamage() {
        return damage;
    }

    @Override
    protected void spawnTrailParticles(Location point) {
        point.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, point, 1, 0.02, 0.02, 0.02, 0.0);
        point.getWorld().spawnParticle(Particle.CHERRY_LEAVES, point, 1, 0.02, 0.02, 0.02, 0.0);
    }

    @Override
    protected void applyHitEffects(Player caster, Player target) {
        target.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                target.getLocation().add(0, 1.0, 0), 8, 0.3, 0.4, 0.3, 0.0);

        hitsThisCast++;

        Location spot = null;
        if (ThreadLocalRandom.current().nextDouble() < feetSpawnChance) {
            spot = findGroundBelow(target.getLocation());
        }
        if (spot == null) {
            spot = findFlowerLocation(caster);
        }
        if (spot != null) {
            growFlower(caster, spot);
        }
    }

    @Override
    public boolean activate(Player player) {
        hitsThisCast = 0;

        boolean cast = super.activate(player);
        if (!cast) return false;

        // Nobody was hit: keep the original single random flower
        if (hitsThisCast == 0) {
            Location spot = findFlowerLocation(player);
            if (spot != null) {
                growFlower(player, spot);
            }
        }
        return true;
    }
    private void growFlower(Player player, Location location) {
        if (location == null) return;

        FlowerKind kind = FlowerKind.random();

        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(kind.material));
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.setBillboard(Display.Billboard.CENTER);
            d.setViewRange(32.0f);
            d.setTeleportDuration(0);
            d.setInvulnerable(true);
            d.setPersistent(false);
            d.setSilent(true);
        });
        EntityCleanupUtils.markAsArenaEntity(display);

        location.getWorld().playSound(location, Sound.BLOCK_GRASS_BREAK, 1.0f, 1.3f);
        location.getWorld().playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.6f, 1.9f);
        location.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                location.clone().add(0, 0.4, 0), 18, 0.4, 0.4, 0.4, 0.0);
        location.getWorld().spawnParticle(Particle.DUST,
                location.clone().add(0, 0.3, 0), 16, 0.35, 0.3, 0.35,
                new Particle.DustOptions(kind.color, 1.1f));

        final FlowerStructure[] structureHolder = new FlowerStructure[1];

        FlowerTrapTask task = new FlowerTrapTask(plugin, player, location, kind, display,
                triggerRadius, flowerLifetimeMs, blueSlowDurationMs,
                redFireDurationMs, redFireTickDamage, redFireIntervalMs,
                yellowEnergyDrain, witheredBlindDurationMs,
                teamManager, combatService, debuffManager, energyManager, matchManager,
                () -> {
                    if (structureHolder[0] != null) {
                        structureManager.unregister(structureHolder[0]);
                    }
                });

        FlowerStructure structure = new FlowerStructure(player, location, task);
        structureHolder[0] = structure;

        structureManager.register(structure);
        task.runTaskTimer(plugin, 0L, 2L);
    }

    private Location findFlowerLocation(Player player) {
        List<Location> points = conePoints(player, false);
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int attempt = 0; attempt < 24; attempt++) {
            Location point = points.get(random.nextInt(points.size()));
            Location ground = findGroundBelow(point);
            if (ground != null) return ground;
        }

        Location fallback = player.getLocation().add(
                player.getLocation().getDirection().setY(0).normalize().multiply(2.0));
        return findGroundBelow(fallback);
    }

    private Location findGroundBelow(Location point) {
        Location start = point.clone().add(0, 2.0, 0);
        RayTraceResult hit = start.getWorld().rayTraceBlocks(start, new Vector(0, -1, 0), 9.0,
                FluidCollisionMode.NEVER, true);

        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() != BlockFace.UP) {
            return null;
        }

        Block above = hit.getHitBlock().getRelative(BlockFace.UP);
        if (!above.isPassable() || !above.getRelative(BlockFace.UP).isPassable()) {
            return null;
        }

        return above.getLocation().add(0.5, 0.05, 0.5);
    }

    @Override
    public String getDescription() {
        return "Releases flowers in a cone, dealing " + (int) damage
                + " damage. A random flower grows where the petals land and debuffs the first enemy who steps on it.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Blue Flower", "Slowness II (" + (blueSlowDurationMs / 1000L) + "s)"),
                new AbilityStat("Red Flower", (int) redFireTickDamage + " fire damage (" + (redFireDurationMs / 1000L) + "s)"),
                new AbilityStat("Yellow Flower", "-" + (int) yellowEnergyDrain + " energy"),
                new AbilityStat("Withered Flower", "Stun (" + (witheredBlindDurationMs / 1000L) + "s)"),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }

    private enum FlowerKind {
        BLUE(Material.CORNFLOWER, Color.fromRGB(70, 120, 255)),
        RED(Material.POPPY, Color.fromRGB(230, 50, 50)),
        YELLOW(Material.DANDELION, Color.fromRGB(245, 220, 60)),
        WITHERED(Material.WITHER_ROSE, Color.fromRGB(90, 70, 95));

        private static final FlowerKind[] VALUES = values();

        private final Material material;
        private final Color color;

        FlowerKind(Material material, Color color) {
            this.material = material;
            this.color = color;
        }

        private static FlowerKind random() {
            return VALUES[ThreadLocalRandom.current().nextInt(VALUES.length)];
        }
    }

    private static final class FlowerTrapTask extends BukkitRunnable {

        private static final long INTERVAL_TICKS = 2L;

        private final Plugin plugin;
        private final Player owner;
        private final Location location;
        private final FlowerKind kind;
        private final ItemDisplay display;
        private final double triggerRadius;
        private final long lifetimeTicks;
        private final long blueSlowDurationMs;
        private final long redFireDurationMs;
        private final double redFireTickDamage;
        private final long redFireIntervalMs;
        private final double yellowEnergyDrain;
        private final long witheredBlindDurationMs;
        private final TeamManager teamManager;
        private final CombatService combatService;
        private final DebuffManager debuffManager;
        private final EnergyManager energyManager;
        private final MatchManager matchManager;
        private final Runnable onEnd;

        private long ticks = 0;
        private boolean ended = false;

        private FlowerTrapTask(Plugin plugin, Player owner, Location location, FlowerKind kind,
                               ItemDisplay display, double triggerRadius, long lifetimeMs,
                               long blueSlowDurationMs, long redFireDurationMs, double redFireTickDamage,
                               long redFireIntervalMs, double yellowEnergyDrain, long witheredBlindDurationMs,
                               TeamManager teamManager, CombatService combatService, DebuffManager debuffManager,
                               EnergyManager energyManager, MatchManager matchManager, Runnable onEnd) {
            this.plugin = plugin;
            this.owner = owner;
            this.location = location;
            this.kind = kind;
            this.display = display;
            this.triggerRadius = triggerRadius;
            this.lifetimeTicks = Math.max(INTERVAL_TICKS, lifetimeMs / 50L);
            this.blueSlowDurationMs = blueSlowDurationMs;
            this.redFireDurationMs = redFireDurationMs;
            this.redFireTickDamage = redFireTickDamage;
            this.redFireIntervalMs = redFireIntervalMs;
            this.yellowEnergyDrain = yellowEnergyDrain;
            this.witheredBlindDurationMs = witheredBlindDurationMs;
            this.teamManager = teamManager;
            this.combatService = combatService;
            this.debuffManager = debuffManager;
            this.energyManager = energyManager;
            this.matchManager = matchManager;
            this.onEnd = onEnd;
        }

        public void deactivate() {
            end(false);
        }

        @Override
        public void run() {
            if (ended) {
                cancel();
                return;
            }

            ticks += INTERVAL_TICKS;

            if (!owner.isOnline() || !display.isValid()) {
                end(false);
                return;
            }

            if (ticks > lifetimeTicks) {
                end(true);
                return;
            }

            drawParticles();

            for (Entity entity : location.getWorld().getNearbyEntities(
                    location, triggerRadius, 2.0, triggerRadius)) {
                if (!(entity instanceof Player target)) continue;
                if (target.equals(owner) || target.isDead()) continue;
                if (!teamManager.isEnemy(owner, target)) continue;

                Location feet = target.getLocation();
                double dx = feet.getX() - location.getX();
                double dz = feet.getZ() - location.getZ();
                if (dx * dx + dz * dz > triggerRadius * triggerRadius) continue;
                if (Math.abs(feet.getY() - location.getY()) > 1.5) continue;

                trigger(target);
                return;
            }
        }

        private void drawParticles() {
            Location base = location.clone().add(0, 0.35, 0);
            base.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, base, 1, 0.12, 0.12, 0.12, 0.0);

            if (ticks % 10 == 0) {
                base.getWorld().spawnParticle(Particle.DUST, base, 3, 0.18, 0.15, 0.18,
                        new Particle.DustOptions(kind.color, 0.9f));
            }
        }

        private void trigger(Player target) {
            switch (kind) {
                case BLUE -> {
                    debuffManager.tryApply(owner, target, DebuffType.SLOW, blueSlowDurationMs);
                    target.getWorld().spawnParticle(Particle.SNOWFLAKE,
                            target.getLocation().add(0, 1.0, 0), 12, 0.3, 0.5, 0.3, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.5f);
                }
                case RED -> {
                    ignite(target);
                    target.getWorld().spawnParticle(Particle.FLAME,
                            target.getLocation().add(0, 1.0, 0), 16, 0.3, 0.5, 0.3, 0.03);
                    target.getWorld().playSound(target.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.2f);
                }
                case YELLOW -> {
                    energyManager.removeEnergy(target, yellowEnergyDrain);
                    target.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                            target.getLocation().add(0, 1.0, 0), 14, 0.3, 0.5, 0.3, 0.05);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 0.6f);
                }
                case WITHERED -> {
                    debuffManager.tryApply(owner, target, DebuffType.STUN, witheredBlindDurationMs);
                    target.getWorld().spawnParticle(Particle.SQUID_INK,
                            target.getLocation().add(0, 1.6, 0), 18, 0.35, 0.35, 0.35, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.8f);
                }
            }

            location.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                    location.clone().add(0, 0.4, 0), 22, 0.4, 0.4, 0.4, 0.02);
            end(false);
        }

        private void ignite(Player target) {
            target.setFireTicks((int) Math.max(1L, redFireDurationMs / 50L));

            long intervalTicks = Math.max(1L, redFireIntervalMs / 50L);
            int hits = (int) Math.max(1L, redFireDurationMs / Math.max(1L, redFireIntervalMs));

            new BukkitRunnable() {
                private int dealt = 0;

                @Override
                public void run() {
                    if (dealt >= hits || !target.isOnline() || target.isDead()
                            || !matchManager.isInMatch(target)) {
                        cancel();
                        return;
                    }

                    Player attacker = owner.isOnline() ? owner : null;
                    combatService.applyAbilityDamage(attacker, target, redFireTickDamage,
                            "Flower Breath", location);
                    target.getWorld().spawnParticle(Particle.FLAME,
                            target.getLocation().add(0, 1.0, 0), 4, 0.25, 0.4, 0.25, 0.01);
                    dealt++;
                }
            }.runTaskTimer(plugin, 0L, intervalTicks);
        }

        private void end(boolean showFade) {
            if (ended) return;
            ended = true;

            if (display.isValid()) {
                if (showFade) {
                    display.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR,
                            display.getLocation().add(0, 0.3, 0), 10, 0.25, 0.25, 0.25, 0.0);
                }
                display.remove();
            }

            if (onEnd != null) {
                onEnd.run();
            }
            cancel();
        }
    }

    private static final class FlowerStructure extends PlacedStructure {

        private final FlowerTrapTask trap;

        private FlowerStructure(Player owner, Location location, FlowerTrapTask trap) {
            super(owner, location);
            this.trap = trap;
        }

        @Override
        public boolean tick() {
            return false;
        }

        @Override
        public void remove() {
            trap.deactivate();
        }

        @Override
        public List<Block> getOccupiedBlocks() {
            return List.of();
        }

        @Override
        public List<Location> getHitLocations() {
            return List.of(getLocation());
        }
    }
}
