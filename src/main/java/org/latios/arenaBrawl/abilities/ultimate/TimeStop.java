package org.latios.arenaBrawl.abilities.ultimate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.UsageManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.UltimateCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.debuffs.DebuffType;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayList;
import java.util.List;

public class TimeStop implements Ability {

    private final long chargeTimeMillis;
    private final long stunDurationMs;
    private final AbilityCost cost;
    private final CooldownManager cooldownManager;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public TimeStop(CooldownManager cooldownManager, UsageManager usageManager, TeamManager teamManager,
                    DebuffManager debuffManager, AbilityConfig config) {
        this.cooldownManager = cooldownManager;
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;

        this.chargeTimeMillis = config.getLong("charge-time-ms", 60000L);
        this.stunDurationMs = config.getLong("stun-duration-ms", 4000L);

        this.cost = new UltimateCost(cooldownManager, usageManager, "timestop");
        this.config = config;
    }

    @Override
    public void onMatchStart(Player player) {
        cooldownManager.setCooldown(player, "timestop", chargeTimeMillis);
    }

    @Override
    public String getName() { return "Time Stop"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        List<Player> victims = new ArrayList<>();
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.equals(player)) continue;
            if (candidate.isDead() || candidate.getGameMode() == GameMode.SPECTATOR) continue;
            if (!teamManager.isEnemy(player, candidate)) continue;
            victims.add(candidate);
        }

        if (victims.isEmpty()) {
            player.sendMessage(MessageUtils.noValidPlayer());
            return false;
        }

        for (Player victim : victims) {
            debuffManager.clear(victim);
            debuffManager.tryApply(victim, DebuffType.STUN, stunDurationMs);
            victim.setVelocity(new Vector(0, 0, 0));
            victim.setFallDistance(0f);

            victim.getWorld().spawnParticle(Particle.END_ROD, victim.getLocation().add(0, 1, 0),
                    40, 0.4, 0.9, 0.4, 0.02);
            victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0),
                    25, 0.35, 0.8, 0.35, 0.01);
            victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 0.6f);
        }

        MatchSoundUtils.play(config, player, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.6f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.7f);

        player.sendMessage("§bYou shattered time! §7" + victims.size()
                + (victims.size() == 1 ? " enemy is" : " enemies are") + " frozen in place.");
        return true;
    }

    @Override
    public String getDescription() {
        return "Freezes time across the entire arena, locking every enemy in place for a few seconds no matter where they are.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Freeze Duration", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Range", "Global"),
                new AbilityStat("Cooldown", (chargeTimeMillis / 1000L) + "s")
        );
    }
}
