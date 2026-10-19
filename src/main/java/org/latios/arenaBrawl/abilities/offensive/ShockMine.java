package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.general.MessageUtils;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ShockMine implements Ability {

    private final Plugin plugin;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final CombatService combatService;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    private final double energyCost;
    private final double damage;
    private final double triggerRadius;
    private final double blastRadius;
    private final long stunDurationMs;
    private final double maxRange;
    private final int maxMines;

    private final Deque<ShockMineTask> activeMines = new ArrayDeque<>();
    public ShockMine(Plugin plugin, EnergyManager energyManager, TeamManager teamManager,
                     CombatService combatService, DebuffManager debuffManager, AbilityConfig config) {
        this.plugin = plugin;
        this.energyCost = config.getDouble("energy-cost", 40.0);
        this.damage = config.getDouble("damage", 120.0);
        this.triggerRadius = config.getDouble("trigger-radius", 2.2);
        this.blastRadius = config.getDouble("blast-radius", 3.5);
        this.stunDurationMs = config.getLong("stun-duration-ms", 1500L);
        this.maxRange = config.getDouble("max-range", 20.0);
        this.maxMines = Math.max(1, config.getInt("max-mines", 6));

        this.cost = new EnergyCost(energyManager, energyCost);
        this.teamManager = teamManager;
        this.combatService = combatService;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Shock Mine"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        RayTraceResult result = player.rayTraceBlocks(maxRange);
        if (result == null || result.getHitBlock() == null) {
            player.sendMessage(MessageUtils.noValidBlock());
            return false;
        }

        BlockFace face = result.getHitBlockFace() != null ? result.getHitBlockFace() : BlockFace.UP;
        Location location = result.getHitBlock().getRelative(face).getLocation().add(0.5, 0.1, 0.5);

        // At the limit, remove the oldest mine (FIFO) instead of blocking the new placement.
        while (activeMines.size() >= maxMines) {
            ShockMineTask oldest = activeMines.pollFirst();
            if (oldest == null) break;
            oldest.deactivate();
        }

        MatchSoundUtils.play(config, player, Sound.BLOCK_TRIPWIRE_ATTACH, 0.9f, 0.6f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_BEACON_POWER_SELECT, 0.7f, 1.7f);

        final ShockMineTask[] holder = new ShockMineTask[1];
        ShockMineTask mine = new ShockMineTask(player, location, damage, triggerRadius, blastRadius, stunDurationMs,
                 teamManager, combatService, debuffManager, () -> activeMines.remove(holder[0]));
        holder[0] = mine;
        activeMines.addLast(mine);
        mine.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Plants a hidden electric mine where you aim. Enemies that step close detonate it, taking area damage and being stunned.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Damage", String.valueOf((int) damage)),
                new AbilityStat("Stun", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Blast Radius", (int) blastRadius + " blocks"),
                new AbilityStat("Max Active", String.valueOf(maxMines)),
                new AbilityStat("Energy Cost", String.valueOf((int) energyCost))
        );
    }
}
