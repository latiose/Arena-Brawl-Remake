
package org.latios.arenaBrawl.abilities.utility;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.abilities.structures.StructureDemolitionService;
import org.latios.arenaBrawl.abilities.structures.StructureManager;
import org.latios.arenaBrawl.general.MovementLockManager;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class BullChargeAbility implements Ability {

    private static final long DURATION_TICKS = 20; // 1 second

    private final AbilityCost cost;

    private final MovementLockManager movementLockManager;
   private final StructureDemolitionService demolitionService;

    public BullChargeAbility(CooldownManager cooldownManager, CombatUpgradeManager upgradeManager,
                             StructureDemolitionService demolitionService, MovementLockManager movementLockManager) {
        this.cost = new CooldownCost(cooldownManager, "bullcharge", 30000, upgradeManager);
        this.movementLockManager = movementLockManager;
        this.demolitionService = demolitionService;
    }

    @Override
    public String getName() { return "Bull Charge"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public String getDescription() {
        return "Charges forward disguised as a cow. Instantly breaks "
                + "structures in your path and stops if you hit a solid block.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Distance", "15 blocks"),
                new AbilityStat("Duration", "1s"),
                new AbilityStat("Breaks structures", "Yes")
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

        movementLockManager.lock(player); // prevents other movement-affecting systems from interfering

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, 1f, 0.7f);
        player.sendMessage("§eMoooove out of the way!");
        new BullChargeTask(player, direction, demolitionService,movementLockManager)
                .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }
}