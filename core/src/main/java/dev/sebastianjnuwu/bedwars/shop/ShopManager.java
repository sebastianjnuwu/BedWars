package dev.sebastianjnuwu.bedwars.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.shop.model.ShopCategory;
import dev.sebastianjnuwu.bedwars.shop.model.ShopItem;
import dev.sebastianjnuwu.bedwars.shop.parser.ShopConfigParser;

/**
 * Gerencia as lojas do plugin.
 * <p>
 * Extrai e carrega os arquivos YAML do diretório {@code shop/}, faz a migração
 * do antigo {@code shop.yml} e mantém um cache das lojas carregadas. O parsing
 * do YAML fica no {@link ShopConfigParser}.
 * </p>
 */
public class ShopManager {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final File shopFolder;
    private final Map<String, List<ShopCategory>> shopCache;
    private final ShopConfigParser parser;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.shopFolder = new File(plugin.getDataFolder(), "shop");
        this.shopFolder.mkdirs();
        this.shopCache = new HashMap<>();
        this.parser = new ShopConfigParser(plugin, this.lang);
    }

    /**
     * Garante que a loja padrão exista e a pré-carrega no cache.
     * <p>
     * Migra o antigo {@code shop.yml} para {@code shop/default.yml} quando o
     * arquivo padrão ainda não existe e extrai o YAML embutido caso nenhum
     * arquivo esteja presente.
     * </p>
     */
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

    /**
     * Carrega uma loja do diretório {@code shop/}, usando o cache quando possível.
     *
     * @param name nome do arquivo da loja (sem extensão)
     * @return categorias da loja (a loja padrão se o arquivo não existir)
     */
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
                ShopCategory category = parser.loadCategory(key, section);
                if (category != null) {
                    categories.add(category);
                }
            }
        }

        shopCache.put(name, categories);
        return categories;
    }

    /**
     * Retorna as categorias de uma loja, carregando-a se necessário.
     *
     * @param shopName nome da loja
     * @return categorias da loja
     */
    public List<ShopCategory> getCategories(String shopName) {
        List<ShopCategory> cached = shopCache.get(shopName);
        if (cached != null) {
            return cached;
        }
        return loadShop(shopName);
    }

    /**
     * Retorna a configuração de níveis de um upgrade da loja.
     *
     * @param shopName nome da loja
     * @param upgrade  identificador do upgrade (ex.: {@code forge})
     * @return configuração de níveis ou {@code null} se não encontrada
     */
    public @Nullable UpgradeConfig getUpgradeConfig(final String shopName, final String upgrade) {
        for (final ShopCategory category : this.getCategories(shopName)) {
            final UpgradeConfig config = findUpgradeConfig(category, upgrade);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    private @Nullable UpgradeConfig findUpgradeConfig(final ShopCategory category, final String upgrade) {
        for (final ShopItem item : category.getItems()) {
            if (upgrade.equalsIgnoreCase(item.getUpgrade())) {
                return item.getUpgradeConfig();
            }
        }
        for (final ShopCategory child : category.getChildren()) {
            final UpgradeConfig config = findUpgradeConfig(child, upgrade);
            if (config != null) {
                return config;
            }
        }
        return null;
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

    /**
     * Invalida o cache de uma loja específica.
     *
     * @param name nome da loja
     */
    public void invalidateCache(String name) {
        shopCache.remove(name);
    }

    /**
     * Invalida o cache de todas as lojas carregadas.
     */
    public void invalidateAll() {
        shopCache.clear();
    }

    /**
     * Retorna o diretório onde as lojas ficam armazenadas.
     *
     * @return pasta {@code shop/}
     */
    public File getShopFolder() {
        return shopFolder;
    }
}
