package org.latios.arenaBrawl.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;
import org.latios.arenaBrawl.abilities.*;
import org.latios.arenaBrawl.runes.RuneConfig;
import org.latios.arenaBrawl.runes.RuneConfigManager;
import org.latios.arenaBrawl.runes.RuneSelectionManager;
import org.latios.arenaBrawl.runes.RuneType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbilitySelectorGUI {

    public static final int INVENTORY_SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45;
    public static final int PREVIOUS_PAGE_SLOT = 48;
    public static final int NEXT_PAGE_SLOT = 50;

    public static final int HATS_SLOT = 29;
    public static final int RUNE_SLOT = 31;
    public static final int UPGRADES_SLOT = 33;

    private static final int ABILITY_ROW = 1;
    private static final int RUNE_ROW = 2;

    private final RuneSelectionManager runeSelectionManager;
    private final AbilityRegistry registry;
    private final AbilitySelectionManager selectionManager;
    private AbilityDependencies previewDependencies;
    private final Map<String, Ability> previewCache = new HashMap<>();
    private final RuneConfigManager runeConfigManager;
    private final AbilityIconRegistry abilityIconRegistry;

    public AbilitySelectorGUI(AbilityRegistry registry, AbilitySelectionManager selectionManager,
                              RuneSelectionManager runeSelectionManager, RuneConfigManager runeConfig,
                              AbilityIconRegistry abilityIconRegistry) {
        this.registry = registry;
        this.selectionManager = selectionManager;
        this.runeSelectionManager = runeSelectionManager;
        this.runeConfigManager = runeConfig;
        this.abilityIconRegistry = abilityIconRegistry;
    }

    public Ability createPreview(AbilitySlot slot, String id) {
        return previewCache.computeIfAbsent(slot.name() + ":" + id, k -> registry.create(slot, id, previewDependencies));
    }

    public void setPreviewDependencies(AbilityDependencies deps) {
        this.previewDependencies = deps;
    }

    public void clearPreviewCache() {
        previewCache.clear();
    }

    public static int getTotalPages(int itemCount) {
        return Math.max(1, (int) Math.ceil(itemCount / (double) ITEMS_PER_PAGE));
    }

    public static int getContentSlot(int index) {
        return index % ITEMS_PER_PAGE;
    }

    public static int getContentIndex(int rawSlot, int page) {
        if (rawSlot < 0 || rawSlot >= ITEMS_PER_PAGE) {
            return -1;
        }
        return page * ITEMS_PER_PAGE + rawSlot;
    }

    public static int getMainMenuSlot(AbilitySlot slot) {
        return centeredRow(ABILITY_ROW, AbilitySlot.values().length)[slot.ordinal()];
    }

    public static AbilitySlot getMainMenuAbilitySlot(int rawSlot) {
        for (AbilitySlot slot : AbilitySlot.values()) {
            if (getMainMenuSlot(slot) == rawSlot) {
                return slot;
            }
        }
        return null;
    }

    public static int getRuneMenuSlot(int runeOrdinal) {
        return centeredRow(RUNE_ROW, RuneType.values().length)[runeOrdinal];
    }

    public static RuneType getRuneAtMenuSlot(int rawSlot) {
        RuneType[] runes = RuneType.values();
        for (int i = 0; i < runes.length; i++) {
            if (getRuneMenuSlot(i) == rawSlot) {
                return runes[i];
            }
        }
        return null;
    }

    public void openSlotMenu(Player player, AbilitySlot slot) {
        openSlotMenu(player, slot, 0);
    }

    public void openSlotMenu(Player player, AbilitySlot slot, int page) {
        List<String> ids = new ArrayList<>(registry.getAvailableIds(slot));
        int totalPages = PaginationUtil.totalPages(ids.size());
        int currentPage = PaginationUtil.clampPage(page, ids.size());

        Inventory inv = Bukkit.createInventory(
                new AbilitySelectorHolder(slot, false, currentPage),
                PaginationUtil.INVENTORY_SIZE,
                PaginationUtil.title("Choose: " + slot.name(), currentPage, totalPages));

        String current = selectionManager.getSelection(player, slot);

        int start = PaginationUtil.firstIndex(currentPage);
        int end = PaginationUtil.lastIndexExclusive(currentPage, ids.size());

        for (int i = start; i < end; i++) {
            String id = ids.get(i);
            boolean selected = id.equals(current);

            Ability preview = createPreview(slot, id);

            ItemStack item = new ItemStack(abilityIconRegistry.getIcon(slot, id));
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName((selected ? "§a✔ " : "§f") + preview.getName());

            if (selected) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            List<String> lore = new ArrayList<>();
            lore.add("");

            List<AbilityStat> stats = preview.getStats();
            if (!stats.isEmpty()) {
                for (AbilityStat stat : stats) {
                    lore.add("§b" + stat.label() + ": §f" + stat.value());
                }
                lore.add("");
            }

            String baseCost = preview.getCost().getBaseCostDescription();
            if (!baseCost.isEmpty()) {
                lore.add("§b" + baseCost);
                lore.add("");
            }

            for (String line : wrapText(preview.getDescription(), 40)) {
                lore.add("§7" + line);
            }

            lore.add("");
            lore.add(selected ? "§aCurrently selected" : "§eClick to select");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(PaginationUtil.slotOf(i), item);
        }

        PaginationUtil.addNavigation(inv, currentPage, totalPages);
        player.openInventory(inv);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new AbilitySelectorHolder(null), INVENTORY_SIZE, "Selection Menu");

        for (AbilitySlot slot : AbilitySlot.values()) {
            String current = selectionManager.getSelection(player, slot);
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + slot.name());
            meta.setLore(List.of("", "§7Current: §f" + prettify(current), "", "§eClick to change"));
            item.setItemMeta(meta);
            inv.setItem(getMainMenuSlot(slot), item);
        }

        ItemStack hatItem = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta hatMeta = hatItem.getItemMeta();
        hatMeta.setDisplayName("§dHats");
        hatMeta.setLore(List.of("", "§7Click to choose your hat"));
        hatItem.setItemMeta(hatMeta);
        inv.setItem(HATS_SLOT, hatItem);

        RuneType currentRune = runeSelectionManager.getSelection(player);
        ItemStack runeItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta runeMeta = runeItem.getItemMeta();
        runeMeta.setDisplayName("§dRune");
        runeMeta.setLore(List.of("", "§7Current: §f" + currentRune.getDisplayName(), "", "§eClick to change"));
        runeItem.setItemMeta(runeMeta);
        inv.setItem(RUNE_SLOT, runeItem);

        ItemStack upgradesItem = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        ItemMeta upgradesMeta = upgradesItem.getItemMeta();
        upgradesMeta.setDisplayName("§6Combat Upgrades");
        upgradesMeta.setLore(List.of("", "§7Click to improve your combat stats"));
        upgradesItem.setItemMeta(upgradesMeta);
        inv.setItem(UPGRADES_SLOT, upgradesItem);

        player.openInventory(inv);
    }

    public void openRuneMenu(Player player) {
        openRuneMenu(player, 0);
    }

    public void openRuneMenu(Player player, int page) {
        RuneType[] runes = RuneType.values();
        int totalPages = PaginationUtil.totalPages(runes.length);
        int currentPage = PaginationUtil.clampPage(page, runes.length);

        Inventory inv = Bukkit.createInventory(
                new AbilitySelectorHolder(null, true, currentPage),
                PaginationUtil.INVENTORY_SIZE,
                PaginationUtil.title("Choose a Rune", currentPage, totalPages));

        RuneType current = runeSelectionManager.getSelection(player);

        int start = PaginationUtil.firstIndex(currentPage);
        int end = PaginationUtil.lastIndexExclusive(currentPage, runes.length);

        for (int i = start; i < end; i++) {
            RuneType rune = runes[i];
            boolean selected = rune == current;

            RuneConfig cfg = runeConfigManager.get(rune.getConfigId());

            ItemStack item = new ItemStack(selected ? Material.LIME_DYE : Material.GRAY_DYE);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName((selected ? "§a✔ " : "§f") + rune.getDisplayName());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§bProc chance: §f" + pct(cfg.getDouble("proc-chance", defaultProcChance(rune))));

            switch (rune) {
                case SPEED -> {
                    lore.add("§bDuration: §f" + seconds(cfg.getLong("duration-ticks", 60)));
                    lore.add("§bSpeed level: §f" + (cfg.getInt("amplifier", 2) + 1));
                }
                case SLOW -> lore.add("§bDuration: §f" + secondsSlow(cfg.getLong("duration-ticks", 3000)));
                case DAMAGE -> lore.add("§bDamage multiplier: §fx" + num(cfg.getDouble("damage-multiplier", 2.0)));
                case ENERGY -> lore.add("§bEnergy gained: §f+" + num(cfg.getDouble("energy-amount", 10.0)));
                default -> { }
            }

            lore.add("");
            lore.add(selected ? "§aCurrently selected" : "§eClick to select");
            meta.setLore(lore);

            item.setItemMeta(meta);
            inv.setItem(PaginationUtil.slotOf(i), item);
        }

        PaginationUtil.addNavigation(inv, currentPage, totalPages);
        player.openInventory(inv);
    }

    private ItemStack createArrow(String name, int targetPage, int totalPages) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("", "§7Page §f" + targetPage + "§7/§f" + totalPages));
        arrow.setItemMeta(meta);
        return arrow;
    }

    private static int[] centeredRow(int row, int count) {
        int spacing = count <= 4 ? 2 : 1;
        int width = (count - 1) * spacing + 1;
        int start = Math.max(0, (9 - width) / 2);
        int[] positions = new int[count];
        for (int i = 0; i < count; i++) {
            positions[i] = row * 9 + start + i * spacing;
        }
        return positions;
    }

    private double defaultProcChance(RuneType rune) {
        return switch (rune) {
            case SPEED -> 0.2;
            case SLOW, ENERGY -> 0.15;
            case DAMAGE -> 0.48;
        };
    }

    private String pct(double value) {
        return num(value * 100) + "%";
    }

    private String seconds(long ticks) {
        return num(ticks / 20.0) + "s";
    }

    private String secondsSlow(long ticks) {
        return num(ticks / 1000.0) + "s";
    }

    private String num(double value) {
        return value == Math.rint(value)
                ? String.valueOf((long) value)
                : String.format(java.util.Locale.US, "%.1f", value);
    }

    private String prettify(String id) {
        return id.substring(0, 1).toUpperCase() + id.substring(1);
    }

    public record AbilitySelectorHolder(AbilitySlot slot, boolean isRuneMenu, int page) implements InventoryHolder {
        public AbilitySelectorHolder(AbilitySlot slot) {
            this(slot, false, 0);
        }

        public AbilitySelectorHolder(AbilitySlot slot, boolean isRuneMenu) {
            this(slot, isRuneMenu, 0);
        }

        @Override
        public @NonNull Inventory getInventory() {
            throw new UnsupportedOperationException();
        }
    }

    public List<String> wrapText(String text, int lineLength) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > lineLength) {
                if (!currentLine.isEmpty()) {
                    result.add(currentLine.toString());
                    currentLine.setLength(0);
                }
            }
            if (!currentLine.isEmpty()) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (!currentLine.isEmpty()) {
            result.add(currentLine.toString());
        }

        return result;
    }
}