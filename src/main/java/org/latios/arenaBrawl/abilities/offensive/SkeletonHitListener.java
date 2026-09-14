package org.latios.arenaBrawl.abilities.offensive;


import org.latios.arenaBrawl.abilities.BaseMinionHitListener;
import org.latios.arenaBrawl.abilities.CooldownManager;

public class SkeletonHitListener extends BaseMinionHitListener {



    public SkeletonHitListener(
            SkeletonEntityManager entityManager,
            CooldownManager cooldownManager
    ) {
        super(entityManager, cooldownManager);
    }


}