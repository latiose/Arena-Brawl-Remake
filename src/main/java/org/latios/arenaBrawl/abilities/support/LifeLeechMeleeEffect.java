package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class LifeLeechMeleeEffect implements MeleeHitEffect {

    private final LifeLeechManager lifeLeechManager;
    private final PlayerHealthManager healthManager;

    public LifeLeechMeleeEffect(LifeLeechManager lifeLeechManager, PlayerHealthManager healthManager) {
        this.lifeLeechManager = lifeLeechManager;
        this.healthManager = healthManager;
    }

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (lifeLeechManager.consumeCharge(attacker)) {
            healthManager.heal(attacker, 60.0, "Life Leech");
        }
    }
    @Override
    public double getMultiplier(Player attacker) {
       return 1;
    }
}