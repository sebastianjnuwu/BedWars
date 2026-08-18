package dev.sebastianjnuwu.bedwars.hook;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
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
                "players",
                "players_playing",
                "players_waiting",
                "spectators",
                "games",
                "games_playing",
                "games_waiting",
                "arena_<nome>",
                "in_game",
                "state",
                "mode",
                "map",
                "team",
                "team_color",
                "team_players",
                "team_eliminated",
                "bed",
                "alive",
                "kills",
                "deaths",
                "code",
                "spectating"
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
            case "players" -> String.valueOf(this.totalPlayers(games));
            case "players_playing" -> String.valueOf(this.playersInState(games, GameState.PLAYING));
            case "players_waiting" -> String.valueOf(this.playersInLobby(games));
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
        if (game == null) {
            return switch (id) {
                case "in_game", "spectating" -> "0";
                case "state" -> "none";
                case "map", "mode", "code", "team", "team_color", "team_players",
                        "team_eliminated", "bed", "alive", "kills", "deaths" -> "";
                default -> null;
            };
        }
        return switch (id) {
            case "in_game" -> "1";
            case "state" -> game.getState().name().toLowerCase(Locale.ROOT);
            case "map" -> game.getArena().getName();
            case "mode" -> this.modeName(game);
            case "code" -> game.getCode();
            case "spectating" -> String.valueOf(game.spectators.contains(player.getUniqueId()) ? 1 : 0);
            default -> this.playerStats(game, player.getUniqueId(), id);
        };
    }

    private @Nullable String playerStats(final Game game, final UUID playerId, final String id) {
        final GamePlayer gp = game.players.get(playerId);
        if (gp == null) {
            return switch (id) {
                case "team", "team_color", "team_players", "team_eliminated",
                        "bed", "alive", "kills", "deaths" -> "";
                default -> null;
            };
        }
        final ArenaTeam team = gp.getTeam();
        return switch (id) {
            case "team" -> team.getName();
            case "team_color" -> team.getColor();
            case "team_players" -> String.valueOf(game.teams.get(team).size());
            case "team_eliminated" -> String.valueOf(game.eliminatedTeams.contains(team) ? 1 : 0);
            case "bed" -> String.valueOf(!game.bedlessTeams.contains(team) ? 1 : 0);
            case "alive" -> String.valueOf(gp.isAlive() ? 1 : 0);
            case "kills" -> String.valueOf(gp.getKills());
            case "deaths" -> String.valueOf(gp.getDeaths());
            default -> null;
        };
    }

    private String modeName(final Game game) {
        final ArenaMode mode = game.mode;
        if (mode == null) {
            return "livre";
        }
        return switch (mode) {
            case SOLO -> "solo";
            case DOUBLES -> "dupla";
            case THREES -> "trio";
            case FOURS -> "quarteto";
        };
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