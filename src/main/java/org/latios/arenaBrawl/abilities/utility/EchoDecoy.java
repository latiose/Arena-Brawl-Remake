package org.latios.arenaBrawl.abilities.utility;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.latios.arenaBrawl.abilities.Ability;
import org.latios.arenaBrawl.abilities.AbilityCost;
import org.latios.arenaBrawl.abilities.AbilityStat;
import org.latios.arenaBrawl.abilities.CooldownManager;
import org.latios.arenaBrawl.abilities.config.AbilityConfig;
import org.latios.arenaBrawl.abilities.cost.CooldownCost;
import org.latios.arenaBrawl.debuffs.DebuffManager;
import org.latios.arenaBrawl.general.MatchSoundUtils;
import org.latios.arenaBrawl.team.TeamManager;
import org.latios.arenaBrawl.upgrades.CombatUpgradeManager;

import java.util.List;

public class EchoDecoy implements Ability {

    private final Plugin plugin;
    private final long cooldownMs;
    private final long durationMs;
    private final double moveSpeed;
    private final double baitRadius;
    private final double surpriseRadius;
    private final long armDelayMs;
    private final long stunDurationMs;
    private final AbilityCost cost;
    private final TeamManager teamManager;
    private final DebuffManager debuffManager;
    private final AbilityConfig config;

    public EchoDecoy(Plugin plugin, CooldownManager cooldownManager, TeamManager teamManager,
                     DebuffManager debuffManager, CombatUpgradeManager combatUpgradeManager, AbilityConfig config) {
        this.plugin = plugin;
        this.cooldownMs = config.getLong("cooldown-ms", 26000L);
        this.durationMs = config.getLong("duration-ms", 6000L);
        this.moveSpeed = config.getDouble("move-speed", 0.32);
        this.baitRadius = config.getDouble("bait-radius", 2.5);
        this.surpriseRadius = config.getDouble("surprise-radius", 5.0);
        this.armDelayMs = config.getLong("arm-delay-ms", 2000L);
        this.stunDurationMs = config.getLong("stun-duration-ms", 2000L);

        this.cost = new CooldownCost(cooldownManager, "echodecoy", cooldownMs, combatUpgradeManager);
        this.teamManager = teamManager;
        this.debuffManager = debuffManager;
        this.config = config;
    }

    @Override
    public String getName() { return "Echo Decoy"; }

    @Override
    public AbilityCost getCost() { return cost; }

    @Override
    public boolean activate(Player player) {
        Location origin = player.getLocation().clone();
        Vector direction = origin.getDirection();
        direction.setY(0);
        if (direction.lengthSquared() < 1.0E-6) {
            direction = new Vector(0, 0, 1);
        }
        direction.normalize();

        ArmorStand decoy = origin.getWorld().spawn(origin, ArmorStand.class, stand -> {
            stand.setArms(true);
            stand.setBasePlate(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setCollidable(false);
            stand.setCustomNameVisible(false);
            stand.setVisible(true);
            stand.setRotation(origin.getYaw(), origin.getPitch());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skull) {
                skull.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
            }
            head.setItemMeta(meta);

            stand.getEquipment().setHelmet(head);
            stand.getEquipment().setChestplate(player.getInventory().getChestplate());
            stand.getEquipment().setLeggings(player.getInventory().getLeggings());
            stand.getEquipment().setBoots(player.getInventory().getBoots());
            stand.getEquipment().setItemInMainHand(player.getInventory().getItemInMainHand());
        });

        MatchSoundUtils.play(config, player, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.9f, 1.3f);
        MatchSoundUtils.play(config, player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);

        new EchoDecoyTask(decoy, player, direction, moveSpeed, baitRadius, surpriseRadius,
                armDelayMs, durationMs, stunDurationMs, teamManager, debuffManager)
                .runTaskTimer(plugin, 1L, 1L);

        return true;
    }

    @Override
    public String getDescription() {
        return "Summons a spectral copy of yourself that runs forward. Enemies that get close are baited as it bursts, and once it has been out for a moment it surprises nearby enemies, stunning everyone caught in range.";
    }

    @Override
    public List<AbilityStat> getStats() {
        return List.of(
                new AbilityStat("Duration", (durationMs / 1000L) + "s"),
                new AbilityStat("Bait Radius", (int) baitRadius + " blocks"),
                new AbilityStat("Surprise Radius", (int) surpriseRadius + " blocks"),
                new AbilityStat("Stun", (stunDurationMs / 1000.0) + "s"),
                new AbilityStat("Cooldown", (cooldownMs / 1000L) + "s")
        );
    }
}
