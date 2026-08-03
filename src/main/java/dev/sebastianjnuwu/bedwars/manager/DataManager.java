package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.api.Saveable;

public class DataManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final ConfigManager configManager;
    private final List<Saveable> saveables = new ArrayList<>();

    public DataManager(final JavaPlugin plugin, final ConfigManager configManager) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configManager = configManager;
    }

    public void register(final Saveable saveable) {
        this.saveables.add(saveable);
    }

    /**
     * Centraliza logs de debug.
     */
    public void debug(final String managerName, final String message) {
        if (this.configManager.isDebugEnabled()) {
            this.logger.info("[BedWars] [DEBUG] [" + managerName + "] - " + message);
        }
    }

    /**
     * Salva todos os componentes registrados de forma assíncrona.
     */
    public void saveAllAsync() {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, this::saveAllSync);
    }

    /**
     * Salva todos os componentes registrados de forma síncrona (usar no onDisable).
     */
    public void saveAllSync() {
        this.debug("DataManager", "Salvando todos os dados...");
        for (final Saveable saveable : this.saveables) {
            try {
                saveable.save();
            } catch (final Exception e) {
                this.logger.severe("Erro ao salvar componente: " + e.getMessage());
            }
        }
        this.debug("DataManager", "Todos os dados foram salvos.");
    }
}
