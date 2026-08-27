package org.latios.arenaBrawl.abilities.offensive;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.ArenaBrawlPlugin;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.cost.EnergyCost;
import org.latios.arenaBrawl.general.CombatService;
import org.latios.arenaBrawl.general.EnergyManager;
import org.latios.arenaBrawl.team.TeamManager;

import java.util.List;

public class MelonLauncher implements Ability {
    private final AbilityCost cost;
    private static final double DAMAGE = 140.0;
    private static final double ENERGY_COST = 60.0;
    private static final double SLICE_DAMAGE = 10.0;
    private static final double AOE_RADIUS = 4.0;
    private final TeamManager teamManager;
    private final CombatService combatService;

    public MelonLauncher(EnergyManager energyManager, TeamManager teamManager, CombatService combatService) {
        this.cost = new EnergyCost(energyManager, ENERGY_COST);
        this.teamManager = teamManager;
        this.combatService = combatService;
    }

    @Override
    public String getName() { return "Melon Launcher"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        ArmorStand melonStand = player.getWorld().spawn(player.getEyeLocation().subtract(0, 1.2, 0), ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.getEquipment();
            stand.getEquipment().setHelmet(new ItemStack(Material.MELON));
        });

        Vector velocity = player.getLocation().getDirection().multiply(1.2);
        velocity.setY(velocity.getY() + 0.1);

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_DISPENSER_LAUNCH, 1, 1.5f);

        new TrackedMelonTask(melonStand, velocity, player, DAMAGE, SLICE_DAMAGE, AOE_RADIUS, getName(), teamManager, combatService)
                .runTaskTimer(ArenaBrawlPlugin.getInstance(), 0L, 1L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Launches a melon that splits into 3 slices on impact.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Main Damage", String.valueOf((int) DAMAGE)),
                new AbilityStat("Slice Damage", String.valueOf((int) SLICE_DAMAGE)),
                new AbilityStat("Explosion Radius", String.valueOf((int) AOE_RADIUS)),
                new AbilityStat("Slices", "3"),
                new AbilityStat("Energy Cost", (int) ENERGY_COST + "")
        );
    }
}