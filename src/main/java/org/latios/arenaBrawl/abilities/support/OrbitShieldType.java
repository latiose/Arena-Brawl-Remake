// abilities/support/OrbitShieldType.java
package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public enum OrbitShieldType {

    BONE_SHIELD(
            "Bone Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.BONE,
            5, 30.0, false,
            18_000, Sound.ENTITY_SKELETON_STEP, Particle.WHITE_ASH,
            2
    ),

    STAR_SHIELD(
            "Star Shield", OrbitShieldVisualType.CHARGED_CREEPER, null,
            3, 50.0, true,
            30_000, Sound.ENTITY_CREEPER_HURT, Particle.END_ROD,
            3
    );

    private final String displayName;
    private final OrbitShieldVisualType visualType;
    private final Material material;
    private final int chargeCount;
    private final double healPerCharge;
    private final boolean rollsDebuffOnBlock;
    private final long durationMillis;
    private final Sound ambientSound;
    private final Particle activationParticle;
    private final int updateIntervalTicks;

    OrbitShieldType(String displayName, OrbitShieldVisualType visualType, Material material,
                    int chargeCount, double healPerCharge, boolean rollsDebuffOnBlock,
                    long durationMillis, Sound ambientSound, Particle activationParticle,
                    int updateIntervalTicks) {
        this.displayName = displayName;
        this.visualType = visualType;
        this.material = material;
        this.chargeCount = chargeCount;
        this.healPerCharge = healPerCharge;
        this.rollsDebuffOnBlock = rollsDebuffOnBlock;
        this.durationMillis = durationMillis;
        this.ambientSound = ambientSound;
        this.activationParticle = activationParticle;
        this.updateIntervalTicks = updateIntervalTicks;
    }

    public String getDisplayName() { return displayName; }
    public OrbitShieldVisualType getVisualType() { return visualType; }
    public Material getMaterial() { return material; }
    public int getChargeCount() { return chargeCount; }
    public double getHealPerCharge() { return healPerCharge; }
    public boolean rollsDebuffOnBlock() { return rollsDebuffOnBlock; }
    public long getDurationMillis() { return durationMillis; }
    public Sound getAmbientSound() { return ambientSound; }
    public Particle getActivationParticle() { return activationParticle; }
    public int getUpdateIntervalTicks() { return updateIntervalTicks; }
}