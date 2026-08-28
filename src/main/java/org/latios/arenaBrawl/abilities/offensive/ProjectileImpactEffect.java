package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Particle;
import org.bukkit.Sound;

public record ProjectileImpactEffect(
        Sound sound,
        float volume,
        float pitch,
        Particle particle,
        int particleCount
) {

    public static final ProjectileImpactEffect DEFAULT = new ProjectileImpactEffect(
            Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1.0f, Particle.EXPLOSION, 1
    );

    public static final ProjectileImpactEffect SNOWBALL = new ProjectileImpactEffect(
            Sound.BLOCK_SNOW_BREAK, 1.0f, 1.2f, Particle.SNOWFLAKE, 15
    );

    public static final ProjectileImpactEffect FIREBALL = new ProjectileImpactEffect(
            Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 0.8f, 1.0f, Particle.EXPLOSION, 1
    );
}