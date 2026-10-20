package org.latios.arenaBrawl.abilities.support;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Individualistic anti-melee self-sustain. While active, enemies that hit the caster in melee take
 * thorn damage and the caster is healed. Resolved from CombatService when a melee hit lands.
 */
public class Thornheart implements Ability {

    private record State(long endMs, double thornDamage, double healPerHit) {}

    private static final Map<UUID, State> ACTIVE = new ConcurrentHashMap<>();

    /** Called by CombatService after a melee hit has been applied. */
    public static void onMeleeDamageTaken(Player victim, Player attacker, PlayerHealthManager healthManager) {
        if (victim == null || attacker == null || attacker.equals(victim)) return;

        State state = ACTIVE.get(victim.getUniqueId());
        if (state == null) return;

        if (System.currentTimeMillis() >= state.endMs()) {
            ACTIVE.remove(victim.getUniqueId());
            return;
        }

        if (attacker.isDead() || !attacker.isOnline()) return;

        int roundedThorns = (int) Math.round(state.thornDamage());
        healthManager.damage(attacker, state.thornDamage(), victim);
        double healed = healthManager.heal(victim, state.healPerHit());

        attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1.0, 0),
                12, 0.3, 0.5, 0.3, 0.15);
        attacker.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, attacker.getLocation().add(0, 1.2, 0),
                4, 0.3, 0.3, 0.3, 0.0);
        victim.getWorld().spawnParticle(Particle.HEART, victim.getLocation().add(0, 1.8, 0),
                2, 0.3, 0.2, 0.3, 0.0);

        victim.sendMessage(MessageUtils.positive() + String.format(
                "§3Your %s hit §3%s §3for §c%d §3damage.",
                "Thornheart", attacker.getName(), roundedThorns
        ));
        attacker.sendMessage(MessageUtils.negative() + String.format(
                "§3%s's %s hit §3you §3for §c%d §3damage.",
                victim.getName(), "Thornheart", roundedThorns
        ));

        if (healed > 0) {
            victim.sendMessage(MessageUtils.positive() + String.format(
                    "§3Your %s healed you for §a%d §3health!",
                    "Thornheart", (int) Math.round(healed)
            ));
        }
    }

    private final long cooldownMs;
    private final long durationMs;
    private final double thornDamage;
    private final double healPerHit;

    private final Plugin plugin;
    private final AbilityCost cost;
    private final PlayerHealthManager healthManager;
    private final AbilityConfig config;

    public Thornheart(Plugin plugin, CooldownManager cooldownManager, PlayerHealthManager healthManager,
                      CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.durationMs = config.getLong("duration-ms", 5000L);
        this.thornDamage = config.getDouble("thorn-damage", 100.0);
        this.healPerHit = config.getDouble("heal-per-hit", 80.0);

        this.cost = new CooldownCost(cooldownManager, "thornheart", cooldownMs, combatUpgradeManager);
        this.healthManager = healthManager;
        this.config = config;
    }

    @Override
    public String getName() {
        return "Thornheart";
    }

    @Override
    public AbilityCost getCost() {
        return cost;
    }

    @Override
    public String getDescription() {
        return "For a few seconds, enemies that hit you in melee take thorn damage and you are healed.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Thorn Damage", (int) thornDamage + ""),
                new AbilityStat("Heal per Hit", (int) healPerHit + " HP"),
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        UUID uuid = player.getUniqueId();
        State state = new State(System.currentTimeMillis() + durationMs, thornDamage, healPerHit);
        ACTIVE.put(uuid, state);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, 10.0f, 1.2f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 10.0f, 1.5f);

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.0, 0),
                20, 0.4, 0.6, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.0, 0),
                15, 0.4, 0.6, 0.4, 0.1);

        player.sendMessage(MessageUtils.positive() + "§3Thornheart is active!");

        long maxTicks = durationMs / 50L;

        new BukkitRunnable() {
            long elapsedTicks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || elapsedTicks >= maxTicks) {
                    ACTIVE.remove(uuid, state);
                    cancel();
                    return;
                }

                Location body = player.getLocation().add(0, 1.0, 0);
                for (int i = 0; i < 6; i++) {
                    double angle = (2 * Math.PI / 6) * i;
                    double x = Math.cos(angle) * 0.6;
                    double z = Math.sin(angle) * 0.6;
                    body.getWorld().spawnParticle(Particle.CRIT, body.clone().add(x, 0, z), 1, 0, 0, 0, 0);
                }

                elapsedTicks += 5;
            }
        }.runTaskTimer(plugin, 0L, 5L);

        return true;
    }
}
