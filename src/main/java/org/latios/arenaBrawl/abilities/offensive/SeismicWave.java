package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.abilities.structures.PlacedStructure;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SeismicWave implements Ability {

    private static final double START_OFFSET = 1.0;
    private static final int RISE_TICKS = 2;
    private static final int HOLD_TICKS = 7;
    private static final int SINK_TICKS = 5;
    private static final int LIFETIME_TICKS = HOLD_TICKS + SINK_TICKS + 1;
    private static final double MAX_RISE = 1.4;
    private static final double RISE_FALLOFF = 0.3;
    private static final double MAX_TILT_DEG = 12.0;
    private static final int MAX_DROP_CENTER = 64;
    private static final int MAX_DROP_SIDE = 3;
    private final double energyCost;
    private final double damage;
    private final double range;
    private final double width;
    private final double speed;
    private final double knockbackHorizontal;
    private final double knockbackVertical;
    private final AbilityConfig config;
    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final StructureDemolitionService demolitionService;

    public SeismicWave(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                       CombatService combatService, StructureDemolitionService demolitionService,
                       AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 100.0);
        this.damage = config.getDouble("damage", 275.0);
        this.range = config.getDouble("range", 10.0);
        this.width = config.getDouble("width", 3.0);
        this.speed = config.getDouble("speed", 1.0);
        this.knockbackHorizontal = config.getDouble("knockback-horizontal", 0.3);
        this.knockbackVertical = config.getDouble("knockback-vertical", 0.4);

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.demolitionService = demolitionService;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Seismic Wave";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation();
        World world = origin.getWorld();

        double yaw = Math.toRadians(origin.getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector perp = new Vector(-dir.getZ(), 0, dir.getX());

        MatchSoundUtils.play(config, player, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.6f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_STONE_BREAK, 1.2f, 0.5f);

        new BukkitRunnable() {
            private double distance = START_OFFSET;
            private int standY = origin.getBlockY();
            private Location lastCenter = origin.clone();
            private final Set<UUID> alreadyHit = new HashSet<>();
            private final Set<Block> risen = new HashSet<>();

            @Override
            public void run() {
                if (distance > range) {
                    finish(player, lastCenter);
                    cancel();
                    return;
                }

                Location flat = origin.clone().add(dir.clone().multiply(distance));
                int cx = flat.getBlockX();
                int cz = flat.getBlockZ();

                if (!world.isChunkLoaded(cx >> 4, cz >> 4)) {
                    finish(player, lastCenter);
                    cancel();
                    return;
                }

                Block centerGround = findGround(world, cx, cz, standY, MAX_DROP_CENTER);
                if (centerGround == null) {
                    finish(player, lastCenter);
                    cancel();
                    return;
                }
                standY = centerGround.getY() + 1;
                if (!world.getBlockAt(cx, standY, cz).isPassable()
                        || !world.getBlockAt(cx, standY + 2, cz).isPassable()) {
                    finish(player, lastCenter);
                    cancel();
                    return;
                }

                Location center = new Location(world, flat.getX(), standY, flat.getZ());
                lastCenter = center.clone();

                raiseColumn(center, perp);
                world.playSound(center, Sound.BLOCK_STONE_BREAK, 0.8f, 0.7f);

                hitEnemies(player, center, dir, perp);
                demolishStructures(player, center);

                distance += speed;
            }

            private void raiseColumn(Location center, Vector perp) {
                int half = (int) Math.floor(width / 2.0);
                for (int offset = -half; offset <= half; offset++) {
                    Location p = center.clone().add(perp.clone().multiply(offset));
                    Block ground = findGround(world, p.getBlockX(), p.getBlockZ(), standY, MAX_DROP_SIDE);
                    if (ground == null || !risen.add(ground)) continue;

                    double rise = MAX_RISE - RISE_FALLOFF * Math.abs(offset)
                            + ThreadLocalRandom.current().nextDouble(-0.1, 0.1);
                    spawnRisingBlock(ground, (float) rise);
                }
            }

            private void hitEnemies(Player caster, Location center, Vector dir, Vector perp) {
                double forwardWindow = speed / 2.0 + 0.6;
                double lateralWindow = width / 2.0 + 0.4;

                for (Player enemy : world.getPlayers()) {
                    if (enemy.isDead() || enemy.getGameMode() == GameMode.SPECTATOR) continue;
                    if (!teamManager.isEnemy(caster, enemy)) continue;
                    if (alreadyHit.contains(enemy.getUniqueId())) continue;

                    Location loc = enemy.getLocation();
                    Vector rel = loc.toVector().subtract(center.toVector());
                    double forward = rel.dot(dir);
                    double lateral = rel.dot(perp);
                    double vertical = loc.getY() - center.getY();

                    if (Math.abs(forward) > forwardWindow || Math.abs(lateral) > lateralWindow) continue;
                    if (vertical < -1.5 || vertical > 2.5) continue;

                    alreadyHit.add(enemy.getUniqueId());
                    combatService.applyAbilityDamage(caster, enemy, damage, getName(), loc);

                    Vector push = dir.clone().multiply(knockbackHorizontal).setY(knockbackVertical);
                    enemy.setVelocity(push);

                    world.playSound(loc, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.8f);
                    world.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.2);
                }
            }

            private void demolishStructures(Player caster, Location center) {
                Location probe = center.clone().add(0, 0.5, 0);
                double radius = width / 2.0 + 1.0;
                for (PlacedStructure structure : demolitionService.findStructuresInRadius(probe, radius)) {
                    if (demolitionService.isEnemyStructure(caster, structure)) {
                        demolitionService.demolish(structure, probe);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Block findGround(World world, int x, int z, int standY, int maxDrop) {
        int minY = Math.max(world.getMinHeight(), standY - maxDrop);
        for (int y = standY + 1; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!block.isPassable() && world.getBlockAt(x, y + 1, z).isPassable()) {
                return block;
            }
        }
        return null;
    }

    private void spawnRisingBlock(Block ground, float rise) {
        World world = ground.getWorld();
        BlockData data = ground.getBlockData();

        Block above = ground.getRelative(BlockFace.UP);
        int skyLight = above.getLightFromSky();
        int blockLight = above.getLightFromBlocks();

        BlockDisplay display = world.spawn(ground.getLocation(), BlockDisplay.class, d -> {
            d.setBlock(data);
            d.setPersistent(false);
            d.setBrightness(new Display.Brightness(blockLight, skyLight));
        });

        world.spawnParticle(Particle.BLOCK, ground.getLocation().add(0.5, 1.05, 0.5),
                12, 0.35, 0.1, 0.35, 0.05, data);

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float tiltX = (float) Math.toRadians(rng.nextDouble(-MAX_TILT_DEG, MAX_TILT_DEG));
        float tiltZ = (float) Math.toRadians(rng.nextDouble(-MAX_TILT_DEG, MAX_TILT_DEG));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) applyPose(display, rise, tiltX, tiltZ, RISE_TICKS);
        }, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (display.isValid()) applyPose(display, 0f, 0f, 0f, SINK_TICKS);
        }, HOLD_TICKS);
        Bukkit.getScheduler().runTaskLater(plugin, display::remove, LIFETIME_TICKS);
    }

    private void applyPose(BlockDisplay display, float y, float tiltX, float tiltZ, int ticks) {
        Quaternionf rotation = new Quaternionf().rotateXYZ(tiltX, 0f, tiltZ);

        Vector3f center = new Vector3f(0.5f, 1f, 0.5f);
        Vector3f rotatedCenter = new Vector3f(center).rotate(rotation);
        Vector3f translation = new Vector3f(0f, y, 0f).add(center).sub(rotatedCenter);

        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(new Transformation(
                translation,
                rotation,
                new Vector3f(1f, 1f, 1f),
                new Quaternionf()
        ));
    }


    private void finish(Player caster, Location end) {
        MatchSoundUtils.play(config, caster, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.2f);

        Location spawn = end.clone().add(0, 1, 0);
        spawn.getWorld().spawn(spawn, Firework.class, firework -> {
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .with(FireworkEffect.Type.CREEPER)
                    .withColor(Color.BLACK)
                    .withFade(Color.fromRGB(50, 50, 50))
                    .flicker(false)
                    .trail(false)
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        });
    }

    @Override
    public String getDescription() {
        return "Sends a wave of rising earth forward, damaging and knocking back enemies and demolishing enemy structures.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Range", String.valueOf((int) range)),
                new AbilityStat("Energy", String.valueOf((int) energyCost))
        );
    }
}