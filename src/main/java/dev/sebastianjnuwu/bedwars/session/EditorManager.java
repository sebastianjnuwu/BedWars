package dev.sebastianjnuwu.bedwars.session;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * Gerencia sessões de edição de arenas.
 * As sessões são salvas em session.yml.
 */
public class EditorManager {

    private final Map<String, UUID> arenaEditors;
    private final Map<UUID, String> playerArenas;

    private final File file;
    private final YamlConfiguration config;


    public EditorManager(final JavaPlugin plugin) {

        this.arenaEditors = new HashMap<>();
        this.playerArenas = new HashMap<>();

        this.file = new File(plugin.getDataFolder(), "session.yml");

        if (!this.file.exists()) {
            try {
                this.file.getParentFile().mkdirs();
                this.file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Erro criando session.yml: " + e.getMessage());
            }
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);

        this.load();
    }


    public boolean startSession(final Player player, final String arenaName) {

        UUID uuid = player.getUniqueId();

        String current = this.playerArenas.get(uuid);

        if (current != null && !current.equalsIgnoreCase(arenaName)) {
            return false;
        }

        UUID owner = this.arenaEditors.get(arenaName);

        if (owner != null && !owner.equals(uuid)) {
            return false;
        }

        this.arenaEditors.put(arenaName, uuid);
        this.playerArenas.put(uuid, arenaName);

        this.save();

        return true;
    }


    public void endSession(final Player player) {

        UUID uuid = player.getUniqueId();

        String arena = this.playerArenas.remove(uuid);

        if (arena == null) {
            return;
        }

        this.arenaEditors.remove(arena);

        this.save();
    }


    public void endSession(final String arenaName) {

        UUID uuid = this.arenaEditors.remove(arenaName);

        if (uuid != null) {
            this.playerArenas.remove(uuid);
        }

        this.save();
    }


    public void clear() {

        this.arenaEditors.clear();
        this.playerArenas.clear();

        this.save();
    }


    public boolean isEditing(final Player player, final String arenaName) {

        UUID owner = this.arenaEditors.get(arenaName);

        return owner != null &&
                owner.equals(player.getUniqueId());
    }


    public boolean isBeingEdited(final String arenaName) {
        return this.arenaEditors.containsKey(arenaName);
    }


    public @Nullable String getEditorName(final String arenaName) {

        UUID uuid = this.arenaEditors.get(arenaName);

        if (uuid == null) {
            return null;
        }

        Player player = Bukkit.getPlayer(uuid);

        return player != null ? player.getName() : "desconhecido";
    }


    public @Nullable String getPlayerArena(final Player player) {
        return this.playerArenas.get(player.getUniqueId());
    }


    private void save() {

        this.config.set("sessions", null);

        for (Map.Entry<String, UUID> entry : this.arenaEditors.entrySet()) {

            this.config.set(
                    "sessions." + entry.getKey(),
                    entry.getValue().toString()
            );
        }

        try {
            this.config.save(this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void load() {

        if (!this.config.contains("sessions")) {
            return;
        }

        for (String arena : this.config.getConfigurationSection("sessions").getKeys(false)) {

            String uuid = this.config.getString("sessions." + arena);

            try {

                UUID player = UUID.fromString(uuid);

                this.arenaEditors.put(arena, player);
                this.playerArenas.put(player, arena);

            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}