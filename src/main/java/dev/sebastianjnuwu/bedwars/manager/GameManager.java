package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;

/**
 * Gerencia todas as partidas ativas de BedWars e o mapeamento de jogadores para partidas.
 * Lida com criação, entrada, saída e limpeza de partidas.
 */
public class GameManager implements dev.sebastianjnuwu.bedwars.api.GameManager {

    private final JavaPlugin plugin;
    private final ArenaManager arenaManager;
    private final ConfigManager configManager;
    private final LangManager lang;
    private final Map<String, Game> games;
    private final Map<UUID, Game> playerGames;
    private final ShopNpcManager shopNpcManager;
    private final EditorManager editorManager;

    public GameManager(final JavaPlugin plugin, final ArenaManager arenaManager, final ConfigManager configManager, final LangManager lang, final EditorManager editorManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.lang = lang;
        this.games = new HashMap<>();
        this.playerGames = new HashMap<>();
        this.shopNpcManager = new ShopNpcManager(plugin);
        this.editorManager = editorManager;
    }

    public JavaPlugin getPlugin() {
        return this.plugin;
    }

    public ShopNpcManager getShopNpcManager() {
        return this.shopNpcManager;
    }

    public LangManager getLang() {
        return this.lang;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    @Override
    public @Nullable dev.sebastianjnuwu.bedwars.api.model.Game getGame(final String arenaName) {
        return this.findFirstByArenaName(arenaName);
    }

    public @Nullable Game getGameByWorld(final String worldName) {
        return this.games.get(worldName);
    }

    @Override
    public @Nullable dev.sebastianjnuwu.bedwars.api.model.Game getPlayerGame(final Player player) {
        return this.playerGames.get(player.getUniqueId());
    }

    @Override
    public boolean isInGame(final Player player) {
        return this.playerGames.containsKey(player.getUniqueId());
    }

    @Override
    public List<String> validateArena(final Arena arena) {
        final List<String> missing = new ArrayList<>();
        if (arena.getArenaSpawn() == null) {
            missing.add(this.lang.raw("game.validate_spawn", arena.getName()));
        }
        if (arena.getTeams().size() < 2) {
            missing.add(this.lang.raw("game.validate_teams", arena.getName()));
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() == null) {
                missing.add(this.lang.raw("game.validate_team_spawn", team.getName()));
            }
            if (team.getBed() == null) {
                missing.add(this.lang.raw("game.validate_team_bed", team.getName()));
            }
            final long forgeCount = arena.getGenerators().stream()
                    .filter(generator -> generator.getType().equalsIgnoreCase("forge"))
                    .filter(generator -> team.getName().equalsIgnoreCase(generator.getTeam()))
                    .count();
            if (forgeCount == 0) {
                missing.add(this.lang.raw("game.validate_team_forge", team.getName()));
            } else if (forgeCount > 1) {
                missing.add(this.lang.raw("game.validate_team_forge_duplicate", team.getName()));
            }
        }
        return missing;
    }

    @Override
    public void joinGame(final Player player, final String arenaName) {
        this.joinGame(player, arenaName, null);
    }

    @Override
    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName) {
        this.joinGame(player, arenaName, teamName, true);
    }

    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName, final boolean teleport) {
        if (this.isInGame(player)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_game"));
            return;
        }
        final Arena arena = this.arenaManager.get(arenaName);
        if (arena == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.arena_not_found", arenaName));
            return;
        }

        if (this.editorManager.isBeingEdited(arenaName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.arena_being_edited", arenaName));
            return;
        }

        Game game = this.findOpenGame(arenaName);
        if (game == null) {
            final Arena instance = this.arenaManager.createInstance(arenaName);
            if (instance == null) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.world_not_ready", arenaName));
                return;
            }
            final List<String> missing = this.validateArena(instance);
            if (!missing.isEmpty()) {
                this.arenaManager.deleteInstanceWorld(instance.getWorldName());
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.not_ready", arenaName));
                for (final String msg : missing) {
                    player.sendMessage(this.lang.text(NamedTextColor.GRAY, "game.missing_entry", msg));
                }
                return;
            }
            game = new Game(this, instance, getShopNpcManager());
            this.games.put(this.gameKey(instance), game);
        }

        if (game.getState() == GameState.ENDING) {
            game.joinAsSpectator(player);
            this.playerGames.put(player.getUniqueId(), game);
            return;
        }

        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            game.joinAsSpectator(player);
            this.playerGames.put(player.getUniqueId(), game);
            return;
        }

        game.join(player, teamName, teleport);
        this.playerGames.put(player.getUniqueId(), game);
    }

    @Override
    public void leaveGame(final Player player) {
        final Game game = (Game) this.playerGames.remove(player.getUniqueId());
        if (game != null) {
            game.leave(player);
            if (game.getPlayers().isEmpty()) {
                this.removeGame(this.gameKey(game.getArena()));
                this.debug("debug.room_closed_empty", game.getArena().getName());
            }
        }
    }

    @Override
    public void startGame(final String arenaName) {
        Game game = this.findOpenGame(arenaName);
        if (game == null) {
            final Arena arena = this.arenaManager.get(arenaName);
            if (arena == null) {
                return;
            }
            final Arena instance = this.arenaManager.createInstance(arenaName);
            if (instance == null) {
                return;
            }
            final List<String> missing = this.validateArena(instance);
            if (!missing.isEmpty()) {
                this.arenaManager.deleteInstanceWorld(instance.getWorldName());
                return;
            }
            game = new Game(this, instance, this.shopNpcManager);
            this.games.put(this.gameKey(instance), game);
        }
        final List<String> missing = this.validateArena(game.getArena());
        if (!missing.isEmpty()) {
            return;
        }
        game.start();
    }

    @Override
    public void removePlayerMapping(final Player player) {
        this.playerGames.remove(player.getUniqueId());
    }

    @Override
    public void removeGame(final String key) {
        Game game = this.games.get(key);
        if (game == null) {
            game = this.findFirstByArenaName(key);
            if (game != null) {
                this.games.remove(this.gameKey(game.getArena()));
            }
        } else {
            this.games.remove(key);
        }
        if (game == null) {
            return;
        }
        final Game removed = game;
        this.shopNpcManager.removeGameNpcs(removed.getArena().getWorldName());
        this.playerGames.values().removeIf(g -> g == removed);
        this.arenaManager.deleteInstanceWorld(removed.getArena().getWorldName());
        this.debug("debug.room_closed", removed.getArena().getName());
    }

    private @Nullable Game findFirstByArenaName(final String arenaName) {
        for (final Game game : this.games.values()) {
            if (game.getArena().getName().equals(arenaName)) {
                return game;
            }
        }
        return null;
    }

    private @Nullable Game findOpenGame(final String arenaName) {
        for (final Game game : this.games.values()) {
            final Arena arena = game.getArena();
            if (arena.getName().equals(arenaName)
                    && (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)
                    && !game.isFull()) {
                return game;
            }
        }
        return null;
    }

    private String gameKey(final Arena arena) {
        final String worldName = arena.getWorldName();
        return worldName != null && !worldName.isBlank() ? worldName : arena.getName();
    }

    private void debug(final String key, final Object... args) {
        if (this.configManager.isDebugEnabled()) {
            Bukkit.getLogger().info("[BedWars] " + this.lang.raw(key, args));
        }
    }

    public void forceEndAll() {
        for (final Game game : new ArrayList<>(this.games.values())) {
            game.forceEnd();
        }
        this.games.clear();
        this.playerGames.clear();
        this.shopNpcManager.removeAll();
    }
}
