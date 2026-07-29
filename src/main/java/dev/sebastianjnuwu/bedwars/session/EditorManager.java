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
import org.bukkit.scheduler.BukkitTask;
import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import org.jetbrains.annotations.Nullable;

/**
 * Gerencia sessões de edição de arenas.
 * As sessões são salvas em session.yml.
 * Enquanto uma sessão está ativa, uma tarefa periódica spawna partículas
 * coloridas acima de cada ponto definido da arena (spawn, spawns dos times,
 * camas e geradores).
 */
public class EditorManager {

    /** Intervalo em ticks entre cada pulso de partículas (4 ticks ≈ 200 ms). */
    private static final long PARTICLE_INTERVAL = 4L;

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final Map<String, UUID>       arenaEditors;
    private final Map<UUID, String>       playerArenas;
    private final Map<String, BukkitTask> particleTasks; // arenaName → task

    private final File               file;
    private final YamlConfiguration  config;

    public EditorManager(final JavaPlugin plugin) {
        this.plugin        = plugin;
        this.lang          = ((BedWarsPlugin) plugin).getLang();
        this.arenaEditors  = new HashMap<>();
        this.playerArenas  = new HashMap<>();
        this.particleTasks = new HashMap<>();

        this.file = new File(plugin.getDataFolder(), "session.yml");

        if (!this.file.exists()) {
            try {
                this.file.getParentFile().mkdirs();
                this.file.createNewFile();
            } catch (final IOException e) {
                plugin.getLogger().severe(this.lang.raw("log.editor_manager.session_create_error", e.getMessage()));
            }
        }

        this.config = YamlConfiguration.loadConfiguration(this.file);
        this.load();
    }

    // ── session lifecycle ────────────────────────────────────────────────

    public boolean startSession(final Player player, final String arenaName) {
        final UUID uuid = player.getUniqueId();

        final String current = this.playerArenas.get(uuid);
        if (current != null && !current.equalsIgnoreCase(arenaName)) {
            return false;
        }

        final UUID owner = this.arenaEditors.get(arenaName);
        if (owner != null && !owner.equals(uuid)) {
            return false;
        }

        this.arenaEditors.put(arenaName, uuid);
        this.playerArenas.put(uuid, arenaName);
        this.save();
        return true;
    }

    /**
     * Starts the particle task for an active editor session.
     * Must be called after startSession, once the ArenaManager is available.
     */
    public void startParticleTask(final Player player,
                                   final String arenaName,
                                   final dev.sebastianjnuwu.bedwars.manager.ArenaManager arenaManager) {
        // Cancel any stale task for this arena first
        this.cancelParticleTask(arenaName);

        final EditorParticleTask runnable = new EditorParticleTask(player, arenaName, arenaManager);
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                this.plugin, runnable, 0L, PARTICLE_INTERVAL);
        this.particleTasks.put(arenaName, task);
    }

    public void endSession(final Player player) {
        final UUID uuid = player.getUniqueId();
        final String arena = this.playerArenas.remove(uuid);
        if (arena == null) return;

        this.arenaEditors.remove(arena);
        this.cancelParticleTask(arena);
        this.save();
    }

    public void endSession(final String arenaName) {
        final UUID uuid = this.arenaEditors.remove(arenaName);
        if (uuid != null) {
            this.playerArenas.remove(uuid);
        }
        this.cancelParticleTask(arenaName);
        this.save();
    }

    public void clear() {
        // Cancel all running particle tasks before clearing
        for (final BukkitTask task : this.particleTasks.values()) {
            task.cancel();
        }
        this.particleTasks.clear();
        this.arenaEditors.clear();
        this.playerArenas.clear();
        this.save();
    }

    // ── queries ──────────────────────────────────────────────────────────

    public boolean isEditing(final Player player, final String arenaName) {
        final UUID owner = this.arenaEditors.get(arenaName);
        return owner != null && owner.equals(player.getUniqueId());
    }

    public boolean isBeingEdited(final String arenaName) {
        return this.arenaEditors.containsKey(arenaName);
    }

    public @Nullable String getEditorName(final String arenaName) {
        final UUID uuid = this.arenaEditors.get(arenaName);
        if (uuid == null) return null;
        final Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getName() : "desconhecido";
    }

    public @Nullable String getPlayerArena(final Player player) {
        return this.playerArenas.get(player.getUniqueId());
    }

    // ── shutdown ─────────────────────────────────────────────────────────

    public void shutdown(final @Nullable dev.sebastianjnuwu.bedwars.manager.ConfigManager configManager,
                         final @Nullable dev.sebastianjnuwu.bedwars.manager.ArenaManager arenaManager,
                         final @Nullable LangManager lang) {
        org.bukkit.Location lobby = configManager != null ? configManager.getLobby() : null;
        if (lobby == null && !Bukkit.getWorlds().isEmpty()) {
            lobby = Bukkit.getWorlds().get(0).getSpawnLocation();
        }

        for (final Map.Entry<String, UUID> entry : new HashMap<>(this.arenaEditors).entrySet()) {
            final String arenaName = entry.getKey();
            final UUID   uuid      = entry.getValue();
            final Player player    = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                if (lobby != null) player.teleport(lobby);
                if (lang != null) {
                    player.sendMessage(lang.raw("edit.session_ended", arenaName));
                }
            }

            if (arenaManager != null) {
                final var arena = arenaManager.get(arenaName);
                if (arena != null) arenaManager.save(arena);
            }
        }

        for (final org.bukkit.World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("bw_")) {
                for (final Player p : world.getPlayers()) {
                    if (lobby != null) p.teleport(lobby);
                }
                Bukkit.unloadWorld(world, false);
            }
        }

        this.clear();
    }

    // ── internal helpers ─────────────────────────────────────────────────

    private void cancelParticleTask(final String arenaName) {
        final BukkitTask task = this.particleTasks.remove(arenaName);
        if (task != null) task.cancel();
    }

    private void save() {
        this.config.set("sessions", null);
        for (final Map.Entry<String, UUID> entry : this.arenaEditors.entrySet()) {
            this.config.set("sessions." + entry.getKey(), entry.getValue().toString());
        }
        try {
            this.config.save(this.file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!this.config.contains("sessions")
                || this.config.getConfigurationSection("sessions") == null) {
            return;
        }
        for (final String arena : this.config.getConfigurationSection("sessions").getKeys(false)) {
            final String uuidStr = this.config.getString("sessions." + arena);
            if (uuidStr == null) continue;
            try {
                final UUID   playerUuid   = UUID.fromString(uuidStr);
                final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    this.arenaEditors.put(arena, playerUuid);
                    this.playerArenas.put(playerUuid, arena);
                }
            } catch (final IllegalArgumentException ignored) {
            }
        }
        this.save();
    }
}