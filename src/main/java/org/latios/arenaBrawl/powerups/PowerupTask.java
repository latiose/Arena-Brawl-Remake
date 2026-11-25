package org.latios.arenaBrawl.powerups;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.cost.EnergyModifierManager;
import org.latios.arenaBrawl.game.Match;
import org.latios.arenaBrawl.game.MatchManager;
import org.latios.arenaBrawl.general.PlayerHealthManager;

import java.util.Map;

public class PowerupTask extends BukkitRunnable {

    private static final double HEAL_AMOUNT = 130.0;
    private static final double DAMAGE_MULTIPLIER = 1.5;
    private static final long DAMAGE_DURATION_MILLIS = 11_000L;
    private static final double ENERGY_MULTIPLIER = 1.5;
    private static final long ENERGY_DURATION_MILLIS = DAMAGE_DURATION_MILLIS;
    private static final String ENERGY_MODIFIER_ID = "energy_powerup";

    private final MatchManager matchManager;
    private final PlayerHealthManager healthManager;
    private final DamageBuffManager damageBuffManager;
    private final EnergyModifierManager energyModifierManager;
    private long lastParticleSecond = -1L;

    public PowerupTask(MatchManager matchManager, PlayerHealthManager healthManager,
                       DamageBuffManager damageBuffManager,
                       EnergyModifierManager energyModifierManager) {
        this.matchManager = matchManager;
        this.healthManager = healthManager;
        this.damageBuffManager = damageBuffManager;
        this.energyModifierManager = energyModifierManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long currentSecond = now / 1000L;
        boolean particleTick = currentSecond != lastParticleSecond;
        lastParticleSecond = currentSecond;

        for (Match match : matchManager.getActiveMatches()) {
            var players = match.getAllPlayers();
            if (players.isEmpty()) continue;

            long elapsed = now - match.getStartedAt();
            PowerupManager powerupManager = match.getPowerupManager();
            powerupManager.tick(elapsed);

            // Trail for players who currently have the energy buff.
            if (particleTick) {
                for (Player p : players) {
                    if (p.isOnline() && energyModifierManager.hasModifier(p, ENERGY_MODIFIER_ID)) {
                        spawnEnergyParticles(p);
                    }
                }
            }

            Map<PowerupType, Player> pickedUp = powerupManager.checkPickups(players);
            for (Map.Entry<PowerupType, Player> entry : pickedUp.entrySet()) {
                Player picker = entry.getValue();
                PowerupType type = entry.getKey();

                // Each player has their own version of the damage powerup:
                // the picker gets the effect of the powerup they actually see.
                PowerupType pickerView = powerupManager.getVisualType(type, picker);

                switch (pickerView) {
                    case HEALTH -> healthManager.heal(picker, HEAL_AMOUNT);
                    case DAMAGE -> damageBuffManager.applyBuff(
                            picker,
                            DAMAGE_MULTIPLIER,
                            DAMAGE_DURATION_MILLIS,
                            PowerupType.DAMAGE.getDisplayName(),
                            true
                    );
                    case ENERGY -> energyModifierManager.addModifier(
                            picker,
                            ENERGY_MODIFIER_ID,
                            ENERGY_MULTIPLIER,
                            ENERGY_DURATION_MILLIS
                    );
                }

                boolean isPickerRed = match.getRed().contains(picker);

                // Announce the powerup that was actually collected: the
                // picker's variant, so everyone reads the same powerup name
                // even when players have different preferences.
                String powerupName = pickerView.getLabel();
                String powerupColor = pickerView.getLegacyColor();

                for (Player p : players) {
                    if (!p.isOnline()) continue;

                    if (p.equals(picker)) {
                        p.sendMessage("§eYou activated the " + powerupColor + "§l" + powerupName + " §epowerup!");
                        if (type == PowerupType.HEALTH) {
                            p.sendMessage("§a+130 health!");
                        }
                    } else {
                        boolean isViewerRed = match.getRed().contains(p);
                        boolean isTeammate = (isPickerRed == isViewerRed);
                        String color = isTeammate ? "§a" : "§c";
                        p.sendMessage(color + picker.getName() + " §eactivated the "
                                + powerupColor + "§l" + powerupName + " §epowerup!");
                    }
                }
            }
        }
    }

    /**
     * Aqua sparks orbiting the player once per second while the energy
     * powerup is active. Only the buffed player sees them, since the
     * powerup is personal.
     */
    private void spawnEnergyParticles(Player player) {
        Location base = player.getLocation().add(0, 1.0, 0);
        double radius = 0.65;
        int points = 8;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2 / points) * i;
            Location point = base.clone().add(
                    Math.cos(angle) * radius,
                    0,
                    Math.sin(angle) * radius
            );
            player.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.02, 0.02, 0.02, 0);
        }

        player.spawnParticle(Particle.END_ROD, base, 2, 0.15, 0.25, 0.15, 0.01);
    }
}
