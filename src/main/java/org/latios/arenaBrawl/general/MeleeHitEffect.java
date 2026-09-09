package org.latios.arenaBrawl.general;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface MeleeHitEffect {
    /**
     * Called every time a melee hit lands successfully, after damage has been applied.
     * Use this for effects that trigger specifically on melee (not projectiles/AoE),
     * such as Life Leech or Berserker's particle/sound flourish.
     */
    void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation);
    double getMultiplier(Player attacker);
}