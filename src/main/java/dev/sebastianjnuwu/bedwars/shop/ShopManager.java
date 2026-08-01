package dev.sebastianjnuwu.bedwars.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

public class ShopManager {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final File shopFolder;
    private final Map<String, List<ShopCategory>> shopCache;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.shopFolder = new File(plugin.getDataFolder(), "shop");
        this.shopFolder.mkdirs();
        this.shopCache = new HashMap<>();
    }

    public void loadDefaults() {
        File defaultShop = new File(shopFolder, "default.yml");

        // Migration: copy old shop.yml to shop/default.yml if it exists
        File oldShop = new File(plugin.getDataFolder(), "shop.yml");
        if (oldShop.exists() && !defaultShop.exists()) {
            try {
                java.nio.file.Files.copy(oldShop.toPath(), defaultShop.toPath());
                plugin.getLogger().info(this.lang.raw("log.shop_manager.migrate_shop"));
                // Don't delete old one, user might want it as reference
            } catch (IOException e) {
                plugin.getLogger().warning(this.lang.raw("log.shop_manager.migrate_shop_error", e.getMessage()));
            }
        }

        // Extract default shop if nothing exists yet
        if (!defaultShop.exists()) {
            try {
                var in = plugin.getResource("shop.yml");
                if (in != null) {
                    java.nio.file.Files.copy(in, defaultShop.toPath());
                }
            } catch (IOException | NullPointerException e) {
                plugin.getLogger().warning(this.lang.raw("log.shop_manager.extract_default_error", e.getMessage()));
            }
        }
        // Pre-load default shop
        loadShop("default");
    }

    public List<ShopCategory> loadShop(String name) {
        List<ShopCategory> cached = shopCache.get(name);
        if (cached != null) {
            return cached;
        }

        File file = new File(shopFolder, name + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning(this.lang.raw("log.shop_manager.not_found", name));
            return loadShop("default");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<ShopCategory> categories = new ArrayList<>();

        ConfigurationSection catsSection = config.getConfigurationSection("categories");
        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                ConfigurationSection section = catsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                ShopCategory category = loadCategory(key, section);
                if (category != null) {
                    categories.add(category);
                }
            }
        }

        shopCache.put(name, categories);
        return categories;
    }

    public List<ShopCategory> getCategories(String shopName) {
        List<ShopCategory> cached = shopCache.get(shopName);
        if (cached != null) {
            return cached;
        }
        return loadShop(shopName);
    }

    /**
     * Retorna o nome de exibicao da loja (titulo da GUI).
     *
     * @param name nome do arquivo da loja
     * @return displayName configurado ou null se nao definido
     */
    public @Nullable String getDisplayName(String name) {
        File file = new File(shopFolder, name + ".yml");
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("displayName");
    }

    public void invalidateCache(String name) {
        shopCache.remove(name);
    }

    public void invalidateAll() {
        shopCache.clear();
    }

    public File getShopFolder() {
        return shopFolder;
    }

    private ShopCategory loadCategory(String name, ConfigurationSection section) {
        String displayName = section.getString("display-name");
        String iconName = section.getString("icon");
        Material icon = iconName != null ? Material.matchMaterial(iconName) : Material.BARRIER;
        List<String> lore = section.getStringList("lore");

        ShopCategory category = new ShopCategory(name, icon, displayName, lore);

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

    private ShopItem parseItem(Map<?, ?> entry) {
        try {
            ShopItem.Builder builder = new ShopItem.Builder();

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

    private void parseLongStack(Map<String, Object> stackMap, ShopItem.Builder builder) {
        if (stackMap.containsKey("type")) {
            Material mat = Material.matchMaterial(stackMap.get("type").toString());
            if (mat != null) {
                builder.material(mat);
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
    }

    private void parseShortStackInternal(String stack, ShopItem.Builder builder) {
        String[] parts = stack.split(";");
        if (parts.length > 0) {
            Material mat = Material.matchMaterial(parts[0].trim());
            if (mat != null) {
                builder.material(mat);
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

    private void parsePrice(String priceStr, ShopItem.Builder builder) {
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

            ShopItem.Builder builder = new ShopItem.Builder();
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
}
