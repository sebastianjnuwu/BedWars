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


    public void shutdown(final @Nullable dev.sebastianjnuwu.bedwars.manager.ConfigManager configManager,
                         final @Nullable dev.sebastianjnuwu.bedwars.manager.ArenaManager arenaManager) {
        org.bukkit.Location lobby = configManager != null ? configManager.getLobby() : null;
        if (lobby == null && !Bukkit.getWorlds().isEmpty()) {
            lobby = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        for (final Map.Entry<String, UUID> entry : new HashMap<>(this.arenaEditors).entrySet()) {
            final String arenaName = entry.getKey();
            final UUID uuid = entry.getValue();
            final Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                if (lobby != null) {
                    player.teleport(lobby);
                }
                player.sendMessage("§cSua sessão de edição na arena '" + arenaName + "' foi encerrada devido ao desligamento do servidor.");
            }

            if (arenaManager != null) {
                final var arena = arenaManager.get(arenaName);
                if (arena != null) {
                    arenaManager.save(arena);
                }
            }
        }

        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("bw_")) {
                for (final Player p : world.getPlayers()) {
                    if (lobby != null) {
                        p.teleport(lobby);
                    }
                }
                Bukkit.unloadWorld(world, false);
            }
        }

        this.clear();
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

        if (!this.config.contains("sessions") || this.config.getConfigurationSection("sessions") == null) {
            return;
        }

        for (final String arena : this.config.getConfigurationSection("sessions").getKeys(false)) {

            final String uuidStr = this.config.getString("sessions." + arena);
            if (uuidStr == null) continue;

            try {

                final UUID player = UUID.fromString(uuidStr);
                final Player onlinePlayer = Bukkit.getPlayer(player);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    this.arenaEditors.put(arena, player);
                    this.playerArenas.put(player, arena);
                }

            } catch (IllegalArgumentException ignored) {
            }
        }
        this.save();
    }
}