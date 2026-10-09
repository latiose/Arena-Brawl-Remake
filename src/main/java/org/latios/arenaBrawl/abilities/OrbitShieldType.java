package org.latios.arenaBrawl.abilities;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

public enum OrbitShieldType {

    BONE_SHIELD(
            "boneshield", "Bone Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.BONE,
            5, 30.0, false,
            18_000, Sound.ENTITY_SKELETON_STEP, Sound.ENTITY_SKELETON_DEATH, Particle.WHITE_ASH,
            2, 0, false
    ),

    CACTUS_SHIELD(
            "cactusshield", "Cactus Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.CACTUS,
            4, 0.0, false,
            18_000, Sound.ENTITY_CHICKEN_AMBIENT, Sound.ENTITY_CHICKEN_DEATH, Particle.WHITE_ASH,
            2, 50, false
    ),

    SPONGE_SHIELD(
            "spongeshield", "Sponge Shield", OrbitShieldVisualType.ITEM_DISPLAY, Material.SPONGE,
            3, 0.0, false,
            18_000, Sound.BLOCK_WET_SPONGE_PLACE, Sound.BLOCK_WET_SPONGE_BREAK, Particle.DRIPPING_WATER,
            2, 0, true
    ),

    STAR_SHIELD(
            "starshield", "Star Shield", OrbitShieldVisualType.CHARGED_CREEPER, null,
            3, 50.0, true,
            30_000, Sound.ENTITY_CREEPER_HURT, Sound.ENTITY_CREEPER_DEATH, Particle.END_ROD,
            3, 0, false
    );

    private final String configKey;
    private final String displayName;
    private final OrbitShieldVisualType visualType;
    private final Material material;

    private int chargeCount;
    private double healPerCharge;
    private boolean rollsDebuffOnBlock;
    private long durationMillis;
    private Sound ambientSound;
    private Sound breakSound;
    private Particle activationParticle;
    private int updateIntervalTicks;
    private double damagePerCharge;
    private boolean knocksback;

    OrbitShieldType(String configKey, String displayName, OrbitShieldVisualType visualType, Material material,
                    int chargeCount, double healPerCharge, boolean rollsDebuffOnBlock,
                    long durationMillis, Sound ambientSound, Sound breakSound, Particle activationParticle,
                    int updateIntervalTicks, double damagePerCharge, boolean knocksback) {
        this.configKey = configKey;
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


    public static void loadFromConfig(FileConfiguration config) {
        for (OrbitShieldType type : values()) {
            String path = "ability-values." + type.configKey + ".";
            if (!config.contains("ability-values." + type.configKey)) continue;

            type.chargeCount = config.getInt(path + "charges", type.chargeCount);
            type.healPerCharge = config.getDouble(path + "heal-per-charge", type.healPerCharge);
            type.damagePerCharge = config.getDouble(path + "damage-per-charge", type.damagePerCharge);
            type.durationMillis = config.getLong(path + "duration-ms", type.durationMillis);
        }
    }

    public String getConfigKey() { return configKey; }
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