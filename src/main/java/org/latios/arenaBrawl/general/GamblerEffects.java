package org.latios.arenaBrawl.general;

import org.bukkit.entity.Player;
import org.latios.arenaBrawl.abilities.OrbitShieldType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GamblerEffects {

    @FunctionalInterface
    public interface Effect {
        void apply(CombatService combat, Player attacker, Player victim, boolean reflected);
    }

    public record Entry(String name, int weight, Effect effect) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static int totalWeight = 0;

    static {
        register("Nothing", 25, (c, a, v, r) -> {});
        register("Damage", 25, (c, a, v, r) ->
                c.reflectDamage(a, v, OrbitShieldType.CACTUS_SHIELD.getDamagePerCharge(), "Gamblers Shield", r));
        register("Knockback", 25, (c, a, v, r) -> c.applyKnockbackEffect(a, v));
        register("Debuff", 25, (c, a, v, r) -> c.applyGuaranteedRandomDebuff(a));
    }

    private GamblerEffects() {}

    public static void register(String name, int weight, Effect effect) {
        ENTRIES.add(new Entry(name, weight, effect));
        totalWeight += weight;
    }

    public static Entry roll() {
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (Entry e : ENTRIES) {
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return ENTRIES.get(0);
    }
}