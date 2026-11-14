package org.latios.arenaBrawl.abilities.offensive;

import org.latios.arenaBrawl.abilities.BaseMinionHitListener;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class KingsGuardHitListener extends BaseMinionHitListener {

    public KingsGuardHitListener(KingsGuardEntityManager entityManager, CooldownManager cooldownManager) {
        super(entityManager, cooldownManager);
    }
}
