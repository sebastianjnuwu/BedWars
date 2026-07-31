package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Gerencia configurações globais do plugin (não por arena). Salva em config.yml
 * na pasta do plugin.
 */
public class ConfigManager {

    private static final Set<Material> FORGE_MATERIALS = Set.of(
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD
    );

    private final JavaPlugin plugin;
    private final File file;
    private final File spawnFile;
    private YamlConfiguration config;
    private YamlConfiguration spawnConfig;
    private Location cachedLobby;
    private LangManager lang;

    /**
     * Cria o gerenciador de configuração global.
     *
     * @param plugin instância do plugin
     */
    public ConfigManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.file = new File(plugin.getDataFolder(), "config.yml");
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        this.load();
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    /**
     * Carrega ou cria o config.yml.
     */
    public void load() {
        if (!this.file.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);

        // Load spawn.yml for lobby
        if (!this.spawnFile.exists()) {
            try {
                this.spawnFile.createNewFile();
            } catch (IOException e) {
                this.plugin.getLogger().warning(this.lang.raw("log.config_manager.create_spawn_error", e.getMessage()));
            }
        }
        this.spawnConfig = YamlConfiguration.loadConfiguration(this.spawnFile);
        this.cachedLobby = loadLobbyFromConfig(this.spawnConfig);

        // Migration: copy lobby from old config.yml to spawn.yml if not yet set
        if (this.cachedLobby == null && this.config.contains("lobby")) {
            try {
                String worldName = this.config.getString("lobby.world");
                if (worldName != null) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Location oldLobby = new Location(
                                world,
                                this.config.getDouble("lobby.x"),
                                this.config.getDouble("lobby.y"),
                                this.config.getDouble("lobby.z"),
                                (float) this.config.getDouble("lobby.yaw"),
                                (float) this.config.getDouble("lobby.pitch")
                        );
                        this.setLobby(oldLobby);
                        this.config.set("lobby", null);
                        this.save();
                        this.plugin.getLogger().info(this.lang.raw("log.config_manager.migrate_lobby"));
                    }
                }
            } catch (Exception e) {
                this.plugin.getLogger().warning(this.lang.raw("log.config_manager.migrate_lobby_error", e.getMessage()));
            }
        }
    }

    /**
     * Salva as alterações no disco.
     */
    public void save() {
        try {
            this.config.save(this.file);
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.config_manager.save_config_error", e.getMessage()));
        }
    }

    public void saveSpawn() {
        if (this.spawnConfig == null) {
            return;
        }
        try {
            this.spawnConfig.save(this.spawnFile);
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.config_manager.save_spawn_error", e.getMessage()));
        }
    }

    /**
     * Define o lobby global do BedWars.
     *
     * @param location local do lobby
     */
    public void setLobby(final Location location) {
        this.spawnConfig.set("lobby.world", location.getWorld().getName());
        this.spawnConfig.set("lobby.x", location.getBlockX());
        this.spawnConfig.set("lobby.y", location.getBlockY());
        this.spawnConfig.set("lobby.z", location.getBlockZ());
        this.spawnConfig.set("lobby.yaw", (double) location.getYaw());
        this.spawnConfig.set("lobby.pitch", (double) location.getPitch());
        this.cachedLobby = location;
        this.saveSpawn();
    }

    /**
     * Retorna o lobby global do BedWars.
     *
     * @return local do lobby ou null se não definido
     */
    public Location getLobby() {
        return this.cachedLobby;
    }

    /**
     * Verifica se o lobby global está configurado.
     *
     * @return true se configurado
     */
    public boolean hasLobby() {
        return this.cachedLobby != null;
    }

    private @Nullable Location loadLobbyFromConfig(YamlConfiguration cfg) {
        if (!cfg.contains("lobby")) {
            return null;
        }
        final String worldName = cfg.getString("lobby.world");
        if (worldName == null) {
            return null;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                cfg.getDouble("lobby.x"),
                cfg.getDouble("lobby.y"),
                cfg.getDouble("lobby.z"),
                (float) cfg.getDouble("lobby.yaw"),
                (float) cfg.getDouble("lobby.pitch")
        );
    }

    public boolean isVersionCheckEnabled() {
        return this.config.getBoolean("check", true);
    }

    public boolean isDebugEnabled() {
        return this.config.getBoolean("debug", false);
    }

    /**
     * Retorna o idioma configurado.
     *
     * @return código do idioma (ex: "pt_BR")
     */
    public String getLang() {
        return this.config.getString("lang", "pt_BR");
    }

    /**
     * Retorna o nível máximo configurado para forjas, com um mínimo seguro de um.
     */
    public int getForgeMaxLevel() {
        return Math.max(1, this.config.getInt("forge.max-level", 1));
    }

    /**
     * Retorna os intervalos de itens, em ticks, para um nível de forja. Entradas
     * inválidas são ignoradas.
     */
    public Map<Material, Long> getForgeIntervals(final int level) {
        final Map<Material, Long> intervals = new EnumMap<>(Material.class);
        final ConfigurationSection section = this.config.getConfigurationSection("forge.levels." + level);
        if (section == null) {
            return intervals;
        }

        for (final String itemName : section.getKeys(false)) {
            final Material material = Material.matchMaterial(itemName + "_INGOT");
            final Material resolved = material != null ? material : Material.matchMaterial(itemName);
            final long interval = section.getLong(itemName + ".interval", 0L);
            if (resolved != null && FORGE_MATERIALS.contains(resolved) && interval > 0L) {
                intervals.put(resolved, interval);
            }
        }
        return intervals;
    }

    /**
     * Retorna o intervalo de queda em ticks para um tipo de gerador global.
     */
    public long getGeneratorInterval(final String type) {
        return this.config.getLong("generators." + type + ".interval", 0L);
    }

    /**
     * Retorna o material configurado para um tipo de gerador global.
     */
    public @Nullable Material getGeneratorMaterial(final String type) {
        final String materialName = this.config.getString("generators." + type + ".material");
        if (materialName == null) {
            return null;
        }
        return Material.matchMaterial(materialName);
    }

}
