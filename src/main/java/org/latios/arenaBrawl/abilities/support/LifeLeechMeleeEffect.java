package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.general.MeleeHitEffect;
import org.latios.arenaBrawl.general.PlayerHealthManager;

public class LifeLeechMeleeEffect implements MeleeHitEffect {

    private final LifeLeechManager lifeLeechManager;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;
    public LifeLeechMeleeEffect(LifeLeechManager lifeLeechManager, PlayerHealthManager healthManager, AbilityConfig config) {
        this.lifeLeechManager = lifeLeechManager;
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public void onMeleeHit(Player attacker, Player victim, double finalDamage, Location impactLocation) {
        if (lifeLeechManager.consumeCharge(attacker)) {
            healthManager.heal(attacker, config.getDouble("heal-per-hit", 60.0), "Life Leech");
        }
    }
    @Override
    public double getMultiplier(Player attacker) {
       return 1;
    }
}