package org.latios.arenaBrawl.debuffs;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class SlowListener implements DebuffListener {
    private static final double SLOW_COEFFICIENT = 0.375;

    private static final NamespacedKey MODIFIER_KEY =
            new NamespacedKey("arenabrawl", "debuff_slow");

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.SLOW) return;

        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) return;

        removeModifier(attribute);

        attribute.addModifier(new AttributeModifier(
                MODIFIER_KEY,
                -SLOW_COEFFICIENT,
                AttributeModifier.Operation.ADD_SCALAR
        ));
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.SLOW) return;

        AttributeInstance attribute = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (attribute == null) return;

        removeModifier(attribute);
    }

    public void removeModifier(AttributeInstance attribute) {
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (modifier.getKey().equals(MODIFIER_KEY)) {
                attribute.removeModifier(modifier);
            }
        }
    }
}