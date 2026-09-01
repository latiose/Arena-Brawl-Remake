package org.latios.arenaBrawl.abilities.utility;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class SalmonForm implements Ability {

    private final long cooldownMs;
    private final long durationTicks;

    private final AbilityCost cost;

    public SalmonForm(CooldownManager cooldownManager, CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.cooldownMs = config.getLong("cooldown-ms", 27_000L);
        this.durationTicks = config.getLong("duration-ticks", 100L);
        this.cost = new CooldownCost(cooldownManager, "salmon_form", cooldownMs, combatUpgradeManager);
    }

    @Override
    public String getName() {
        return "Salmon Form";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "Transforms you into a cute little salmon for 5 seconds, boosting your speed and reducing your size to dodge incoming abilities.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationTicks / 20L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Location startLoc = player.getLocation();

        MobDisguise salmonDisguise = new MobDisguise(DisguiseType.SALMON);
        salmonDisguise.setViewSelfDisguise(false);
        DisguiseAPI.disguiseToAll(player, salmonDisguise);

        player.getWorld().playSound(startLoc, Sound.ENTITY_SALMON_FLOP, 1.2f, 1.0f);
        player.getWorld().playSound(startLoc, Sound.ITEM_BUCKET_FILL_FISH, 1.0f, 1.2f);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, (int) durationTicks, 1, false, false, false));

        new BukkitRunnable() {
            int ticksElapsed = 0;

            @Override
            public void run() {
                ticksElapsed += 2;

                if (!player.isOnline() || player.isDead() || ticksElapsed >= durationTicks) {
                    if (DisguiseAPI.isDisguised(player)) {
                        DisguiseAPI.undisguiseToAll(player);
                    }

                    if (player.isOnline() && !player.isDead()) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_SALMON_AMBIENT, 1.0f, 1.5f);
                        player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 0.5, 0), 20, 0.3, 0.3, 0.3, 0.1);
                    }

                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 0.2, 0);
                loc.getWorld().spawnParticle(Particle.DRIPPING_WATER, loc, 3, 0.2, 0.1, 0.2, 0.0);
            }
        }.runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 2L);

        return true;
    }
}