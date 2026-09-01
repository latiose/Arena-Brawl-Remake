package org.latios.arenaBrawl.abilities.utility;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.general.MovementLockManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class BullCharge implements Ability {

    private final long cooldownMs;
    private final AbilityCost cost;
    private final MovementLockManager movementLockManager;
    private final StructureDemolitionService demolitionService;
    private final Plugin plugin;

    public BullCharge(Plugin plugin, CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                      StructureDemolitionService demolitionService, MovementLockManager movementLockManager,
                      AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 30000L);
        this.cost = new CooldownCost(cooldownManager, "bullcharge", cooldownMs, upgradeManager);
        this.movementLockManager = movementLockManager;
        this.demolitionService = demolitionService;
    }

    @Override
    public String getName() { return "Bull Charge"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Charges forward disguised as a cow. Instantly breaks structures in your path and stops if you hit a solid block.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Distance", "15 blocks"),
                new AbilityStat("Duration", "1s"),
                new AbilityStat("Breaks structures", "Yes"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }

    @Override
    public boolean activate(Player player) {
        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        if (direction.lengthSquared() < 1e-6) direction = new Vector(0, 0, 1);

        MobDisguise disguise = new MobDisguise(DisguiseType.COW);
        disguise.setReplaceSounds(true);
        disguise.setViewSelfDisguise(false);
        disguise.setHideArmorFromSelf(true);
        DisguiseAPI.disguiseToAll(player, disguise);

        movementLockManager.lock(player);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, 1f, 0.7f);
        player.sendMessage("§eMoooove out of the way!");
        new BullChargeTask(player, direction, demolitionService, movementLockManager)
                .runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}