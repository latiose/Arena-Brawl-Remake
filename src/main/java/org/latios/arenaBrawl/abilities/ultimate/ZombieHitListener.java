package org.latios.arenaBrawl.abilities.ultimate;

import org.latios.arenaBrawl.abilities.BaseMinionHitListener;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class ZombieHitListener extends BaseMinionHitListener {
    public ZombieHitListener(ZombieEntityManager entityManager, CooldownManager cooldownManager) {
        super(entityManager, cooldownManager);
    }
}