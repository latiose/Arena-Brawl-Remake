package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

public enum OrbitShieldType {

    BONE_SHIELD(
            "Bone Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.BONE,
            5, 30.0, false,
            18_000, Sound.ENTITY_SKELETON_STEP, Sound.ENTITY_SKELETON_DEATH, Particle.WHITE_ASH,
            2, 0, false
    ),

    CACTUS_SHIELD(
            "Cactus Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.CACTUS,
            4, 0.0, false,
            18_000, Sound.ENTITY_CHICKEN_AMBIENT, Sound.ENTITY_CHICKEN_DEATH, Particle.WHITE_ASH,
            2, 50, false
    ),

    SPONGE_SHIELD(
            "Sponge Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.SPONGE,
            3, 0.0, false,
            30_000, Sound.BLOCK_WET_SPONGE_PLACE, Sound.BLOCK_WET_SPONGE_BREAK, Particle.DRIPPING_WATER,
            2, 0, true
    ),

    STAR_SHIELD(
            "Star Shield", OrbitShieldVisualType.CHARGED_CREEPER, null,
            3, 50.0, true,
            30_000, Sound.ENTITY_CREEPER_HURT, Sound.ENTITY_CREEPER_DEATH, Particle.END_ROD,
            3, 0, false
    );

    private final String displayName;
    private final OrbitShieldVisualType visualType;
    private final Material material;
    private final int chargeCount;
    private final double healPerCharge;
    private final boolean rollsDebuffOnBlock;
    private final long durationMillis;
    private final Sound ambientSound;
    private final Sound breakSound;
    private final Particle activationParticle;
    private final int updateIntervalTicks;
    private final double damagePerCharge;
    private final boolean knocksback;

    OrbitShieldType(String displayName, OrbitShieldVisualType visualType, Material material,
                    int chargeCount, double healPerCharge, boolean rollsDebuffOnBlock,
                    long durationMillis, Sound ambientSound, Sound breakSound, Particle activationParticle,
                    int updateIntervalTicks, double damagePerCharge, boolean knocksback) {
        this.displayName = displayName;
        this.visualType = visualType;
        this.material = material;
        this.chargeCount = chargeCount;
        this.healPerCharge = healPerCharge;
        this.rollsDebuffOnBlock = rollsDebuffOnBlock;
        this.durationMillis = durationMillis;
        this.ambientSound = ambientSound;
        this.breakSound = breakSound;
        this.activationParticle = activationParticle;
        this.updateIntervalTicks = updateIntervalTicks;
        this.damagePerCharge = damagePerCharge;
        this.knocksback = knocksback;
    }

    public String getDisplayName() { return displayName; }
    public OrbitShieldVisualType getVisualType() { return visualType; }
    public Material getMaterial() { return material; }
    public int getChargeCount() { return chargeCount; }
    public double getHealPerCharge() { return healPerCharge; }
    public boolean rollsDebuffOnBlock() { return rollsDebuffOnBlock; }
    public long getDurationMillis() { return durationMillis; }
    public Sound getAmbientSound() { return ambientSound; }
    public Sound getBreakSound() { return breakSound; }
    public Particle getActivationParticle() { return activationParticle; }
    public int getUpdateIntervalTicks() { return updateIntervalTicks; }
    public double getDamagePerCharge() { return damagePerCharge; }
    public boolean doesKnockback() { return knocksback; }
}