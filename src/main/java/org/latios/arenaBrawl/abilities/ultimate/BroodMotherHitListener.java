package org.latios.arenaBrawl.abilities.ultimate;

import org.latios.arenaBrawl.abilities.BaseMinionHitListener;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class BroodMotherHitListener extends BaseMinionHitListener {
    public BroodMotherHitListener(BroodMotherEntityManager entityManager, CooldownManager cooldownManager) {
        super(entityManager, cooldownManager);
    }
}