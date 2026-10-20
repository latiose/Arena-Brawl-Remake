package org.latios.arenaBrawl.abilities.ultimate;

import org.latios.arenaBrawl.abilities.BaseMinionHitListener;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class GuardianColossusHitListener extends BaseMinionHitListener {

    public GuardianColossusHitListener(GuardianColossusEntityManager entityManager,
                                       CooldownManager cooldownManager) {
        super(entityManager, cooldownManager);
    }
}
