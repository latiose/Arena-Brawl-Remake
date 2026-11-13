package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.entity.Player;

/**
 * A structure that can be damaged by melee hits.
 * <p>
 * Implementations are handled generically by {@link StructureHitListener}, so adding a new
 * destructible structure no longer requires editing the listener with another {@code instanceof}
 * branch.
 */
public interface DestructibleStructure {

    /** Applies one melee hit. Returns {@code true} when the structure should be removed. */
    boolean registerHit(Player attacker);

    /** Whether the attacker is allowed to damage this structure. */
    boolean canHit(Player attacker);

    /** Remaining health as a percentage, used for feedback messages. */
    int getHealthPercentage();

    /** Display name used in feedback messages. */
    String getStructureName();

    /**
     * Optional ray-based hit test for structures without solid blocks (for example particle-only
     * screens). Called by {@link StructureHitListener} on left-click.
     */
    default boolean isHitByRay(Player player, double maxDistance) {
        return false;
    }
}
