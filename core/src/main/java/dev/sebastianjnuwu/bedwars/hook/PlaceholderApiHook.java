package dev.sebastianjnuwu.bedwars.hook;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderApiHook extends PlaceholderExpansion {

    private static final String ARENA_PREFIX = "arena_";

    private final JavaPlugin plugin;
    private final GameManager gameManager;

    public PlaceholderApiHook(final JavaPlugin plugin, final GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public String getIdentifier() {
        return "bedwars";
    }

    @Override
    public String getAuthor() {
        return "sebastianjnuwu";
    }

    @Override
    public String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public List<String> getPlaceholders() {
        return List.of(
                "all_players",
                "playing",
                "waiting",
                "spectators",
                "games",
                "games_playing",
                "games_waiting",
                "arena_<nome>",
                "in_game",
                "state",
                "team",
                "code",
                "arena"
        );
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final String params) {
        if (params == null) {
            return null;
        }
        final String id = params.toLowerCase(Locale.ROOT);
        final Collection<Game> games = this.gameManager.getActiveGames();
        return switch (id) {
            case "all_players" -> String.valueOf(this.totalPlayers(games));
            case "playing" -> String.valueOf(this.playersInState(games, GameState.PLAYING));
            case "waiting" -> String.valueOf(this.playersInLobby(games));
            case "spectators" -> String.valueOf(this.totalSpectators(games));
            case "games" -> String.valueOf(games.size());
            case "games_playing" -> String.valueOf(this.countInState(games, GameState.PLAYING));
            case "games_waiting" -> String.valueOf(this.countInLobby(games));
            default -> this.resolve(player, id, games);
        };
    }

    private @Nullable String resolve(final OfflinePlayer player, final String id, final Collection<Game> games) {
        if (id.startsWith(ARENA_PREFIX)) {
            return String.valueOf(this.arenaPlayers(games, id.substring(ARENA_PREFIX.length())));
        }
        return this.resolvePlayer(player, id);
    }

    private @Nullable String resolvePlayer(final OfflinePlayer player, final String id) {
        if (player == null) {
            return null;
        }
        final Game game = this.gameManager.getPlayerGame(player.getUniqueId());
        return switch (id) {
            case "in_game" -> String.valueOf(game != null ? 1 : 0);
            case "state" -> game != null ? game.getState().name().toLowerCase(Locale.ROOT) : "none";
            case "team" -> this.teamName(game, player);
            case "code" -> game != null ? game.getCode() : "";
            case "arena" -> game != null ? game.getArena().getName() : "";
            default -> null;
        };
    }

    private String teamName(final Game game, final OfflinePlayer player) {
        if (game == null) {
            return "";
        }
        final GamePlayer gp = game.players.get(player.getUniqueId());
        return gp != null ? gp.getTeam().getName() : "";
    }

    private int totalPlayers(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            total += game.players.size() + game.spectators.size();
        }
        return total;
    }

    private int totalSpectators(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            total += game.spectators.size();
        }
        return total;
    }

    private int playersInState(final Collection<Game> games, final GameState state) {
        int total = 0;
        for (final Game game : games) {
            if (game.getState() == state) {
                total += game.players.size();
            }
        }
        return total;
    }

    private int playersInLobby(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            final GameState state = game.getState();
            if (state == GameState.WAITING || state == GameState.STARTING) {
                total += game.players.size();
            }
        }
        return total;
    }

    private int countInState(final Collection<Game> games, final GameState state) {
        int total = 0;
        for (final Game game : games) {
            if (game.getState() == state) {
                total++;
            }
        }
        return total;
    }

    private int countInLobby(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            final GameState state = game.getState();
            if (state == GameState.WAITING || state == GameState.STARTING) {
                total++;
            }
        }
        return total;
    }

    private int arenaPlayers(final Collection<Game> games, final String arenaName) {
        int total = 0;
        for (final Game game : games) {
            if (game.getArena().getName().equalsIgnoreCase(arenaName)) {
                total += game.players.size() + game.spectators.size();
            }
        }
        return total;
    }
}