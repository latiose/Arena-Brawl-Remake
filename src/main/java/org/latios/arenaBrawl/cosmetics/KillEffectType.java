package org.latios.arenaBrawl.cosmetics;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.general.EntityCleanupUtils;

/**
 * Purely cosmetic effects that play where an enemy dies. None of these
 * effects deal damage, destroy blocks or set fire to the arena.
 */
public enum KillEffectType {

    LIGHTNING("Thunder Strike", 1250, Material.LIGHTNING_ROD,
            "A pair of harmless lightning bolts crash down."),
    TNT("TNT Blast", 2000, Material.TNT,
            "A TNT block explodes without breaking anything."),
    HEARTS("Heart Burst", 2500, Material.POPPY,
            "Rings of hearts float up from the corpse."),
    FIREWORK("Firework Finale", 3500, Material.FIREWORK_ROCKET,
            "A small firework show celebrates your kill."),
    SOULS("Soul Escape", 4500, Material.SOUL_LANTERN,
            "The victim's soul drifts away in blue flames."),
    ICE("Frozen Shatter", 5500, Material.BLUE_ICE,
            "A freezing shockwave of ice and snow."),
    PORTAL("Void Portal", 6500, Material.ENDER_EYE,
            "A purple portal opens where your enemy fell."),
    SHOCKWAVE("Sonic Boom", 8000, Material.SCULK_SHRIEKER,
            "A warden-style sonic shockwave ripples outwards."),
    METEOR("Meteor Strike", 10000, Material.MAGMA_BLOCK,
            "A blazing meteor crashes onto the death spot."),
    BATS("Bat Swarm", 12000, Material.BAT_SPAWN_EGG,
            "A swarm of bats scatters from the body."),
    VILLAGER_PARTY("Villager Party", 14000, Material.EMERALD,
            "A joyful party of villagers, notes and hearts."),
    CHERRY("Cherry Storm", 16000, Material.CHERRY_SAPLING,
            "A storm of cherry petals and love."),
    DRAGON("Dragon's Wrath", 18000, Material.DRAGON_HEAD,
            "Dragon breath and a sonic boom tear through the air."),
    BLACK_HOLE("Black Hole", 20000, Material.OBSIDIAN,
            "A collapsing black hole drags particles inwards."),
    STORM("Thunderstorm", 22500, Material.TRIDENT,
            "A whole thunderstorm erupts around the kill."),
    GOLDEN("Golden Glory", 25000, Material.GOLD_BLOCK,
            "A legendary golden celebration of your victory.");

    private final String displayName;
    private final int price;
    private final Material icon;
    private final String description;

    KillEffectType(String displayName, int price, Material icon, String description) {
        this.displayName = displayName;
        this.price = price;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrice() {
        return price;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getColor() {
        if (price >= 20000) return "§d";
        if (price >= 10000) return "§6";
        if (price >= 5000) return "§b";
        return "§a";
    }

    public String getRarity() {
        if (price >= 20000) return "Legendary";
        if (price >= 10000) return "Epic";
        if (price >= 5000) return "Rare";
        return "Common";
    }

    public void play(Plugin plugin, Player killer, Location deathLocation) {
        World world = deathLocation.getWorld();
        if (world == null) return;

        Location center = deathLocation.clone().add(0, 0.8, 0);

        switch (this) {
            case LIGHTNING -> {
                world.strikeLightningEffect(center);
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 22, 1.2, 1.6, 1.2, 0.12);
                world.spawnParticle(Particle.END_ROD, center, 10, 0.7, 1.4, 0.7, 0.05);
                CosmeticEffects.sound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.3f, 1.0f);
            }
            case TNT -> {
                TNTPrimed tnt = world.spawn(center, TNTPrimed.class, entity -> {
                    entity.setFuseTicks(80);
                    entity.setGravity(false);
                    entity.setVelocity(new Vector(0, 0, 0));
                    entity.setSilent(true);
                });
                EntityCleanupUtils.markAsArenaEntity(tnt);
                CosmeticEffects.later(plugin, 10L, () -> {
                    if (!tnt.isDead()) tnt.remove();
                });
                world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                world.spawnParticle(Particle.EXPLOSION, center, 8, 1.2, 0.6, 1.2, 0.0);
                world.spawnParticle(Particle.LAVA, center, 14, 1.3, 0.6, 1.3, 0.0);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 10, 1.0, 0.6, 1.0, 0.04);
                CosmeticEffects.sound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.9f);
            }
            case HEARTS -> {
                for (int i = 0; i < 2; i++) {
                    CosmeticEffects.ring(center, Particle.HEART, 0.8 + i * 0.6, 8 + i * 4, 0.2 + i * 0.4);
                }
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 14, 0.7, 1.0, 0.7, 0.1);
                CosmeticEffects.sound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.6f);
            }
            case FIREWORK -> {
                for (int i = 0; i < 2; i++) {
                    Location spawn = center.clone().add(
                            CosmeticEffects.random(-1.0, 1.0),
                            CosmeticEffects.random(0.4, 1.2),
                            CosmeticEffects.random(-1.0, 1.0)
                    );
                    FireworkEffect effect = FireworkEffect.builder()
                            .with(FireworkEffect.Type.BALL)
                            .withColor(CosmeticEffects.randomColor())
                            .withFade(CosmeticEffects.randomColor())
                            .withTrail()
                            .build();
                    world.spawn(spawn, Firework.class, firework -> {
                        FireworkMeta meta = firework.getFireworkMeta();
                        meta.addEffect(effect);
                        meta.setPower(0);
                        firework.setFireworkMeta(meta);
                    });
                }
                CosmeticEffects.sound(center, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.1f, 1.0f);
            }
            case SOULS -> {
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= 14) {
                            cancel();
                            return;
                        }
                        for (int i = 0; i < 2; i++) {
                            Location point = center.clone().add(
                                    CosmeticEffects.random(-0.9, 0.9),
                                    CosmeticEffects.random(0, 1.2),
                                    CosmeticEffects.random(-0.9, 0.9)
                            );
                            world.spawnParticle(Particle.SOUL, point, 1, 0.05, 0.05, 0.05, 0.02);
                            world.spawnParticle(Particle.SOUL_FIRE_FLAME, point, 1, 0.05, 0.05, 0.05, 0.01);
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
                world.spawnParticle(Particle.REVERSE_PORTAL, center, 20, 0.8, 1.2, 0.8, 0.04);
                CosmeticEffects.sound(center, Sound.PARTICLE_SOUL_ESCAPE, 1.1f, 0.8f);
            }
            case ICE -> {
                CosmeticEffects.ring(center, Particle.SNOWFLAKE, 1.0, 18);
                CosmeticEffects.ring(center, Particle.SNOWFLAKE, 1.8, 24);
                CosmeticEffects.dust(center, 26, 0.9, Color.fromRGB(180, 230, 255), 1.2f);
                CosmeticEffects.sound(center, Sound.BLOCK_GLASS_BREAK, 1.1f, 1.4f);
                CosmeticEffects.sound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1.0f, 0.8f);
            }
            case PORTAL -> {
                CosmeticEffects.ring(center, Particle.PORTAL, 1.1, 18);
                CosmeticEffects.ring(center, Particle.REVERSE_PORTAL, 1.7, 22);
                world.spawnParticle(Particle.END_ROD, center, 12, 0.6, 1.4, 0.6, 0.04);
                CosmeticEffects.sound(center, Sound.BLOCK_PORTAL_TRIGGER, 0.9f, 0.7f);
            }
            case SHOCKWAVE -> {
                world.spawnParticle(Particle.SONIC_BOOM, center, 1, 0, 0, 0, 0);
                for (int i = 0; i < 2; i++) {
                    CosmeticEffects.ring(center, Particle.SWEEP_ATTACK, 0.8 + i * 0.8, 10 + i * 2);
                }
                world.spawnParticle(Particle.CLOUD, center, 16, 1.2, 0.3, 1.2, 0.1);
                CosmeticEffects.sound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.2f, 1.0f);
            }
            case METEOR -> {
                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (ticks >= 8) {
                            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                            world.spawnParticle(Particle.EXPLOSION, center, 8, 1.4, 0.7, 1.4, 0.0);
                            world.spawnParticle(Particle.LAVA, center, 16, 1.5, 0.7, 1.5, 0.0);
                            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 10, 1.0, 0.6, 1.0, 0.04);
                            CosmeticEffects.sound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.3f, 0.8f);
                            cancel();
                            return;
                        }

                        Location meteor = center.clone().add(0, 10.0 - (ticks * 1.2), 0);
                        world.spawnParticle(Particle.FLAME, meteor, 8, 0.45, 0.45, 0.45, 0.02);
                        world.spawnParticle(Particle.LAVA, meteor, 2, 0.25, 0.25, 0.25, 0.0);
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, meteor, 2, 0.25, 0.25, 0.25, 0.02);
                        CosmeticEffects.sound(meteor, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.7f);
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
            case BATS -> {
                for (int i = 0; i < 4; i++) {
                    Bat bat = world.spawn(center, Bat.class);
                    bat.setSilent(true);
                    bat.setInvulnerable(true);
                    EntityCleanupUtils.markAsArenaEntity(bat);
                    bat.setVelocity(new Vector(
                            CosmeticEffects.random(-0.6, 0.6),
                            CosmeticEffects.random(0.2, 0.6),
                            CosmeticEffects.random(-0.6, 0.6)
                    ));
                    CosmeticEffects.later(plugin, 50L, () -> {
                        if (!bat.isDead()) bat.remove();
                    });
                }
                CosmeticEffects.sound(center, Sound.ENTITY_BAT_TAKEOFF, 1.3f, 1.0f);
            }
            case VILLAGER_PARTY -> {
                CosmeticEffects.ring(center, Particle.HAPPY_VILLAGER, 0.8, 8);
                CosmeticEffects.ring(center, Particle.HAPPY_VILLAGER, 1.5, 12, 0.5);
                world.spawnParticle(Particle.NOTE, center, 18, 1.1, 1.2, 1.1, 0.25);
                world.spawnParticle(Particle.HEART, center, 6, 0.8, 0.8, 0.8, 0.0);
                CosmeticEffects.sound(center, Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.4f);
                CosmeticEffects.sound(center, Sound.BLOCK_NOTE_BLOCK_PLING, 0.9f, 1.8f);
            }
            case CHERRY -> {
                for (int i = 0; i < 2; i++) {
                    CosmeticEffects.ring(center, Particle.CHERRY_LEAVES, 0.8 + i * 0.7, 10 + i * 4, i * 0.4);
                }
                world.spawnParticle(Particle.HEART, center, 6, 0.8, 1.0, 0.8, 0.0);
                CosmeticEffects.sound(center, Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 1.0f, 1.2f);
            }
            case DRAGON -> {
                for (int i = 0; i < 2; i++) {
                    world.spawnParticle(Particle.DRAGON_BREATH, center, 6, 0.25, 0.3, 0.25, 0.02, 1.0f);
                }
                world.spawnParticle(Particle.SONIC_BOOM, center, 1, 0, 0, 0, 0);
                world.spawnParticle(Particle.END_ROD, center, 14, 0.7, 1.4, 0.7, 0.06);
                CosmeticEffects.sound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.1f, 1.0f);
                CosmeticEffects.sound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.8f);
            }
            case BLACK_HOLE -> {
                CosmeticEffects.sound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.5f);
                new BukkitRunnable() {
                    int ticks = 0;
                    double angle = 0;

                    @Override
                    public void run() {
                        if (ticks >= 20) {
                            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
                            world.spawnParticle(Particle.REVERSE_PORTAL, center, 20, 1.0, 1.0, 1.0, 0.12);
                            CosmeticEffects.sound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
                            cancel();
                            return;
                        }

                        double radius = 2.0 * (1.0 - (ticks / 20.0));
                        for (int i = 0; i < 6; i++) {
                            double pointAngle = angle + i * Math.PI / 3.0;
                            Location point = center.clone().add(
                                    Math.cos(pointAngle) * radius,
                                    CosmeticEffects.random(-0.6, 0.6) * (1.0 - (ticks / 25.0)),
                                    Math.sin(pointAngle) * radius
                            );
                            world.spawnParticle(Particle.PORTAL, point, 1, 0, 0, 0, 0.05);
                            world.spawnParticle(Particle.REVERSE_PORTAL, point, 1, 0, 0, 0, 0.02);
                        }
                        angle += 0.55;
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);
            }
            case STORM -> {
                for (int i = 0; i < 2; i++) {
                    Location strike = center.clone().add(
                            CosmeticEffects.random(-2.0, 2.0),
                            0,
                            CosmeticEffects.random(-2.0, 2.0)
                    );
                    world.strikeLightningEffect(strike);
                }
                world.spawnParticle(Particle.CLOUD, center, 24, 2.0, 0.4, 2.0, 0.04);
                world.spawnParticle(Particle.ELECTRIC_SPARK, center, 24, 1.8, 1.8, 1.8, 0.15);
                CosmeticEffects.sound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.4f, 0.8f);
                CosmeticEffects.sound(center, Sound.WEATHER_RAIN, 0.8f, 1.0f);
            }
            case GOLDEN -> {
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 30, 1.0, 1.5, 1.0, 0.18);
                CosmeticEffects.ring(center, Particle.END_ROD, 1.2, 14);
                CosmeticEffects.ring(center, Particle.FIREWORK, 1.8, 16, 0.5);
                world.spawnParticle(Particle.NOTE, center, 10, 1.0, 1.2, 1.0, 0.25);
                CosmeticEffects.sound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.1f, 1.0f);
            }
        }
    }
}