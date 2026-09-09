package org.latios.arenaBrawl.abilities.structures;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StructureHitListener implements Listener {

    private static final long HIT_COOLDOWN_MILLIS = 500;

    private final StructureManager structureManager;
    private final Map<UUID, Long> lastHitAt = new HashMap<>();

    public StructureHitListener(StructureManager structureManager) {
        this.structureManager = structureManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // Buscar si el bloque hecho clic pertenece a alguna estructura activa
        PlacedStructure structure = findStructureByBlock(clickedBlock);
        if (structure == null) return;

        Action action = event.getAction();

        // 1. Bloquear interactuar con clic derecho en cualquier estructura rompible (Mesa de Encantamientos, Totems, etc.)
        if (action == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            return;
        }

        // 2. Procesar únicamente el clic izquierdo
        if (action != Action.LEFT_CLICK_BLOCK) return;

        event.setCancelled(true);

        Player attacker = event.getPlayer();

        // Cooldown por jugador
        long last = lastHitAt.getOrDefault(attacker.getUniqueId(), 0L);
        if (System.currentTimeMillis() - last < HIT_COOLDOWN_MILLIS) return;
        lastHitAt.put(attacker.getUniqueId(), System.currentTimeMillis());

        // Evaluar tipo de estructura
        if (structure instanceof TreeOfLifeStructure tree) {
            handleTreeOfLifeHit(attacker, tree);
        } else if (structure instanceof HealingTotemStructure totem) {
            handleHealingTotemHit(attacker, totem);
        } else if (structure instanceof MagicTableStructure table) {
            handleMagicTableHit(attacker, table);
        }
    }

    private void handleTreeOfLifeHit(Player attacker, TreeOfLifeStructure tree) {
        boolean canHit = tree.canHit(attacker);
        boolean destroyed = tree.registerHit(attacker);

        if (destroyed) {
            attacker.sendMessage("§eTree of Life destroyed!");
            structureManager.remove(tree);
        } else if (canHit) {
            int healthPercent = tree.getHealthPercentage();
            attacker.sendMessage(String.format("§eTree of Life health: §6%d%%", healthPercent));
        }
    }

    private void handleHealingTotemHit(Player attacker, HealingTotemStructure totem) {
        boolean canHit = totem.canHit(attacker);
        boolean destroyed = totem.registerHit(attacker);

        if (destroyed) {
            attacker.sendMessage("§eHealing Totem destroyed!");
            structureManager.remove(totem);
        } else if (canHit) {
            int healthPercent = totem.getHealthPercentage();
            attacker.sendMessage(String.format("§eHealing Totem health: §6%d%%", healthPercent));
        }
    }

    private void handleMagicTableHit(Player attacker, MagicTableStructure table) {
        boolean canHit = table.canHit(attacker);
        boolean destroyed = table.registerHit(attacker);

        if (destroyed) {
            attacker.sendMessage("§eMagic Table destroyed!");
            structureManager.remove(table);
        } else if (canHit) {
            int healthPercent = table.getHealthPercentage();
            attacker.sendMessage(String.format("§eMagic Table health: §6%d%%", healthPercent));
        }
    }

    private PlacedStructure findStructureByBlock(Block block) {
        for (PlacedStructure structure : structureManager.getAll()) {
            if (structure.getOccupiedBlocks().contains(block)) {
                return structure;
            }
        }
        return null;
    }
}