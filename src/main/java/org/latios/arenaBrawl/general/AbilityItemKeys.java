package org.latios.arenaBrawl.general;

import org.bukkit.NamespacedKey;
import org.latios.arenaBrawl.ArenaBrawlPlugin;

public class AbilityItemKeys {
    public static final NamespacedKey ABILITY_SLOT = new NamespacedKey(
            ArenaBrawlPlugin.getInstance(), "ability_slot"
    );

    public static final NamespacedKey PROJECTILE_DAMAGE = new NamespacedKey(
            ArenaBrawlPlugin.getInstance(), "projectile_damage"
    );

    public static final NamespacedKey PROJECTILE_SOURCE_ABILITY = new NamespacedKey(
            ArenaBrawlPlugin.getInstance(), "projectile_source_ability"
    );
    public static final NamespacedKey PROJECTILE_AOE_RADIUS = new NamespacedKey(
            ArenaBrawlPlugin.getInstance(), "projectile_aoe_radius"
    );
    public static final NamespacedKey ARENA_ENTITY_MARKER = new NamespacedKey(
            ArenaBrawlPlugin.getInstance(), "arena_entity_marker"
    );
}