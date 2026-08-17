package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Responsável por converter as seções YAML das lojas em objetos
 * {@link ShopCategory} e {@link ShopItem}.
 * <p>
 * Centraliza o parsing de categorias, itens no formato longo (mapa), curto
 * (string), preços, moedas e configuração de upgrades.
 * </p>
 */
class ShopConfigParser {

    private final JavaPlugin plugin;
    private final LangManager lang;

    ShopConfigParser(final JavaPlugin plugin, final LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    /**
     * Carrega uma categoria (ou subcategoria) a partir da sua seção de configuração.
     *
     * @param name    nome da categoria
     * @param section seção YAML da categoria
     * @return categoria carregada
     */
    ShopCategory loadCategory(String name, ConfigurationSection section) {
        String displayName = section.getString("display-name");
        String iconName = section.getString("icon");
        Material icon = iconName != null ? Material.matchMaterial(iconName) : Material.BARRIER;
        List<String> lore = section.getStringList("lore");

        String layoutType = "row";
        boolean centered = false;
        ConfigurationSection positioning = section.getConfigurationSection("positioning");
        if (positioning != null) {
            String type = positioning.getString("type");
            if (type != null && !type.isBlank()) {
                layoutType = type.toLowerCase();
            }
            centered = positioning.getBoolean("center", false);
        }

        ShopCategory category = new ShopCategory(name, icon, displayName, lore, layoutType, centered);

        ConfigurationSection catsSection = section.getConfigurationSection("categories");
        if (catsSection != null) {
            for (String subKey : catsSection.getKeys(false)) {
                ConfigurationSection subSection = catsSection.getConfigurationSection(subKey);
                if (subSection != null) {
                    ShopCategory subCategory = loadCategory(subKey, subSection);
                    if (subCategory != null) {
                        category.addChild(subCategory);
                    }
                }
            }
        }

        List<Map<?, ?>> itemsList = section.getMapList("items");
        if (itemsList != null) {
            for (Map<?, ?> entry : itemsList) {
                ShopItem item = parseItem(entry);
                if (item != null) {
                    category.addItem(item);
                }
            }
        }

        List<String> stringItems = section.getStringList("items");
        if (stringItems != null && !stringItems.isEmpty() && (itemsList == null || itemsList.isEmpty())) {
            for (String line : stringItems) {
                ShopItem item = parseShortStack(line);
                if (item != null) {
                    category.addItem(item);
                }
            }
        }

        return category;
    }

    /**
     * Converte um mapa YAML de um item da loja em um {@link ShopItem}.
     *
     * @param entry mapa do item
     * @return item parseado ou {@code null} em caso de erro
     */
    private ShopItem parseItem(Map<?, ?> entry) {
        try {
            ShopItemBuilder builder = new ShopItemBuilder();

            if (entry.containsKey("stack")) {
                Object stackObj = entry.get("stack");
                if (stackObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> stackMap = (Map<String, Object>) stackObj;
                    parseLongStack(stackMap, builder);
                } else if (stackObj instanceof String) {
                    parseShortStackInternal((String) stackObj, builder);
                }
            }

            if (entry.containsKey("price")) {
                Object priceObj = entry.get("price");
                if (priceObj instanceof String) {
                    parsePrice((String) priceObj, builder);
                }
            }

            if (entry.containsKey("upgrade")) {
                builder.upgrade(entry.get("upgrade").toString());
            }

            if (entry.containsKey("upgrade-config")) {
                Object upgradeConfigObj = entry.get("upgrade-config");
                if (upgradeConfigObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> upgradeConfigMap = (Map<String, Object>) upgradeConfigObj;
                    builder.upgradeConfig(parseUpgradeConfig(upgradeConfigMap));
                }
            }

            if (entry.containsKey("skip")) {
                builder.skip(Integer.parseInt(entry.get("skip").toString()));
            }
            if (entry.containsKey("column")) {
                Object col = entry.get("column");
                if (col instanceof String) {
                    String colStr = (String) col;
                    switch (colStr.toLowerCase()) {
                        case "left" -> builder.column(0);
                        case "center" -> builder.column(4);
                        case "right" -> builder.column(8);
                        default -> { }
                    }
                } else {
                    builder.column(Integer.parseInt(col.toString()));
                }
            }
            if (entry.containsKey("row")) {
                builder.row(Integer.parseInt(entry.get("row").toString()));
            }
            if (entry.containsKey("linebreak")) {
                builder.linebreak(entry.get("linebreak").toString());
            }
            if (entry.containsKey("pagebreak")) {
                builder.pagebreak(entry.get("pagebreak").toString());
            }
            if (entry.containsKey("absolute")) {
                builder.absolute(Integer.parseInt(entry.get("absolute").toString()));
            }

            return builder.build();
        } catch (Exception e) {
            plugin.getLogger().warning(this.lang.raw("log.shop_manager.parse_error", String.valueOf(entry), e.getMessage()));
            return null;
        }
    }

    /**
     * Parseia o mapa {@code stack:} de um item da loja (formato longo).
     *
     * @param stackMap mapa do stack
     * @param builder  builder a ser preenchido
     */
    private void parseLongStack(Map<String, Object> stackMap, ShopItemBuilder builder) {
        if (stackMap.containsKey("type")) {
            String type = stackMap.get("type").toString();
            List<Material> armorSet = armorSetFor(type);
            if (armorSet != null) {
                builder.material(armorSet.get(1));
                builder.armorSet(armorSet);
            } else {
                Material mat = Material.matchMaterial(type);
                if (mat != null) {
                    builder.material(mat);
                }
            }
        }
        if (stackMap.containsKey("amount")) {
            builder.amount(Integer.parseInt(stackMap.get("amount").toString()));
        }
        if (stackMap.containsKey("display-name")) {
            builder.displayName(stackMap.get("display-name").toString());
        }
        if (stackMap.containsKey("lore")) {
            Object loreObj = stackMap.get("lore");
            if (loreObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> loreList = (List<String>) loreObj;
                builder.lore(loreList);
            }
        }
        if (stackMap.containsKey("enchants")) {
            Object enchObj = stackMap.get("enchants");
            if (enchObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> enchMap = (Map<String, Object>) enchObj;
                Map<String, Integer> enchants = new HashMap<>();
                for (var entry : enchMap.entrySet()) {
                    enchants.put(entry.getKey(), Integer.parseInt(entry.getValue().toString()));
                }
                builder.enchants(enchants);
            }
        }
        if (stackMap.containsKey("tag")) {
            builder.tag(stackMap.get("tag").toString());
        }
        if (stackMap.containsKey("items")) {
            Object itemsObj = stackMap.get("items");
            if (itemsObj instanceof List) {
                List<ShopItem> contents = new ArrayList<>();
                for (Object entry : (List<?>) itemsObj) {
                    if (entry instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> childMap = (Map<String, Object>) entry;
                        ShopItem child = parseItem(childMap);
                        if (child != null) {
                            contents.add(child);
                        }
                    }
                }
                builder.contents(contents);
            }
        }
    }

    private void parseShortStackInternal(String stack, ShopItemBuilder builder) {
        String[] parts = stack.split(";");
        if (parts.length > 0) {
            String type = parts[0].trim();
            List<Material> armorSet = armorSetFor(type);
            if (armorSet != null) {
                builder.material(armorSet.get(1));
                builder.armorSet(armorSet);
            } else {
                Material mat = Material.matchMaterial(type);
                if (mat != null) {
                    builder.material(mat);
                }
            }
        }
        if (parts.length > 1) {
            builder.amount(Integer.parseInt(parts[1].trim()));
        }
        if (parts.length > 2 && !parts[2].trim().isEmpty()) {
            builder.displayName(parts[2].trim());
        }
        if (parts.length > 3 && !parts[3].trim().isEmpty()) {
            builder.lore(List.of(parts[3].trim()));
        }
    }

    private void parsePrice(String priceStr, ShopItemBuilder builder) {
        priceStr = priceStr.trim();
        String[] parts = priceStr.split(" ");
        if (parts.length >= 2) {
            try {
                builder.price(Integer.parseInt(parts[0]));
                builder.currency(parseCurrency(parts[1]));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private ShopItem parseShortStack(String line) {
        try {
            String[] parts = line.split(" for ");
            if (parts.length != 2) {
                return null;
            }

            ShopItemBuilder builder = new ShopItemBuilder();
            parseShortStackInternal(parts[0].trim(), builder);
            parsePrice(parts[1].trim(), builder);
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    private CurrencyType parseCurrency(String name) {
        return switch (name.toLowerCase()) {
            case "iron" -> CurrencyType.IRON;
            case "gold" -> CurrencyType.GOLD;
            case "diamond" -> CurrencyType.DIAMOND;
            case "emerald" -> CurrencyType.EMERALD;
            default -> CurrencyType.IRON;
        };
    }

    private UpgradeConfig parseUpgradeConfig(Map<?, ?> map) {
        int maxLevel = parsePositiveInt(map.get("max-level"), 1);
        int levelDefault = parsePositiveInt(map.get("level-default"), 0);
        List<ForgeLevel> levels = new ArrayList<>();
        Object levelsObj = map.get("levels");
        if (levelsObj instanceof Map) {
            for (var levelEntry : ((Map<?, ?>) levelsObj).entrySet()) {
                ForgeLevel level = parseUpgradeLevel(levelEntry.getKey(), levelEntry.getValue());
                if (level != null) {
                    levels.add(level);
                }
            }
        }
        return new UpgradeConfig(maxLevel, levelDefault, levels);
    }

    private @Nullable ForgeLevel parseUpgradeLevel(final Object levelKey, final Object value) {
        final int level;
        try {
            level = Integer.parseInt(String.valueOf(levelKey));
        } catch (NumberFormatException e) {
            return null;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map<?, ?> levelMap = (Map<?, ?>) value;
        int upgradePrice = 0;
        Material upgradeMaterial = null;
        Map<Material, Long> intervals = new HashMap<>();
        for (var entry : levelMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.equalsIgnoreCase("upgrade") && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> upgradeMap = (Map<String, Object>) entry.getValue();
                upgradePrice = parsePositiveInt(upgradeMap.get("price"), 0);
                String materialName = upgradeMap.get("material") != null ? upgradeMap.get("material").toString() : null;
                upgradeMaterial = materialName != null ? parseUpgradeMaterial(materialName) : null;
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> intervalMap = (Map<String, Object>) entry.getValue();
                long interval = parsePositiveLong(intervalMap.get("interval"), 0L);
                if (interval > 0L) {
                    Material material = parseUpgradeMaterial(key);
                    if (material != null) {
                        intervals.put(material, interval);
                    }
                }
            }
        }
        if (upgradePrice <= 0 && upgradeMaterial == null && intervals.isEmpty()) {
            return null;
        }
        return new ForgeLevel(level, intervals, upgradePrice, upgradeMaterial);
    }

    private static int parsePositiveInt(final Object value, final int defaultValue) {
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    private static long parsePositiveLong(final Object value, final long defaultValue) {
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    private static @Nullable Material parseUpgradeMaterial(final String name) {
        return switch (name.trim().toLowerCase()) {
            case "iron", "iron_ingot" -> Material.IRON_INGOT;
            case "gold", "gold_ingot" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };
    }

    private static List<Material> armorSetFor(String type) {
        return switch (type.toUpperCase()) {
            case "LEATHER" -> List.of(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
                    Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS);
            case "CHAINMAIL" -> List.of(Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE,
                    Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS);
            case "IRON" -> List.of(Material.IRON_HELMET, Material.IRON_CHESTPLATE,
                    Material.IRON_LEGGINGS, Material.IRON_BOOTS);
            case "GOLD", "GOLDEN" -> List.of(Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE,
                    Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS);
            case "NETHERITE" -> List.of(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS);
            default -> null;
        };
    }
}