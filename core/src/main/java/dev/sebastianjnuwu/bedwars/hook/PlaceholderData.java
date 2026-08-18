package dev.sebastianjnuwu.bedwars.hook;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;

final class PlaceholderData {

    private static final String BED_SYMBOL = "\uD83D\uDECF";
    private static final String[] ARENA_PROPS = {
            "min_players", "max_players", "time_formatted",
            "players", "spectators", "total", "name", "state", "world", "time"
    };
    private static final String[] TEAM_PROPS = {
            "max_players", "bed_symbol", "colored", "players", "alive", "color", "name", "bed"
    };
    private static final String[] MODE_PROPS = {
            "games_waiting", "games_playing", "max_players",
            "players", "spectators", "total", "games"
    };

    private final GameManager gameManager;

    PlaceholderData(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Nullable
    String server(final String id, final Collection<Game> games) {
        return switch (id) {
            case "players" -> String.valueOf(this.serverPlayers(games));
            case "spectators" -> String.valueOf(this.serverSpectators(games));
            case "total_players" -> String.valueOf(this.serverPlayers(games) + this.serverSpectators(games));
            case "games" -> String.valueOf(games.size());
            case "games_playing" -> String.valueOf(this.countInState(games, GameState.PLAYING));
            case "games_waiting" -> String.valueOf(this.countInLobby(games));
            case "games_ending" -> String.valueOf(this.countInState(games, GameState.ENDING));
            case "max_players" -> String.valueOf(this.serverCapacity(games));
            case "any_playing" -> String.valueOf(this.anyInState(games, GameState.PLAYING));
            case "any_waiting" -> String.valueOf(this.anyInLobby(games));
            default -> id.startsWith("mode_") ? this.mode(games, id.substring("mode_".length())) : null;
        };
    }

    @Nullable
    private String mode(final Collection<Game> games, final String rest) {
        final String prop = this.matchProp(rest, MODE_PROPS);
        if (prop == null) {
            return null;
        }
        final String modeKey = rest.substring(0, rest.length() - prop.length() - 1);
        final boolean libre = modeKey.equals("livre");
        final ArenaMode mode = libre ? null : ArenaMode.fromAlias(modeKey);
        if (!libre && mode == null) {
            return null;
        }
        return switch (prop) {
            case "players" -> String.valueOf(this.modePlayers(games, mode, libre));
            case "spectators" -> String.valueOf(this.modeSpectators(games, mode, libre));
            case "total" -> String.valueOf(this.modePlayers(games, mode, libre) + this.modeSpectators(games, mode, libre));
            case "games" -> String.valueOf(this.modeGames(games, mode, libre));
            case "games_playing" -> String.valueOf(this.modeGamesInState(games, mode, libre, GameState.PLAYING));
            case "games_waiting" -> String.valueOf(this.modeGamesInLobby(games, mode, libre));
            case "max_players" -> String.valueOf(this.modeCapacity(games, mode, libre));
            default -> null;
        };
    }

    @Nullable
    String playerOrArena(final OfflinePlayer player, final String id, final Collection<Game> games) {
        if (id.startsWith("arena_")) {
            return this.arena(games, id.substring("arena_".length()));
        }
        if (player == null) {
            return null;
        }
        final Game game = this.gameManager.getPlayerGame(player.getUniqueId());
        if (game == null) {
            return this.outsideGame(id);
        }
        return switch (id) {
            case "in_game" -> "true";
            case "arena" -> game.arena.getName();
            case "state" -> game.getState().name().toLowerCase(Locale.ROOT);
            case "game_players" -> String.valueOf(game.players.size());
            case "min_players" -> String.valueOf(this.minPlayers(game));
            case "game_max_players" -> String.valueOf(this.capacity(game));
            case "time" -> String.valueOf(this.timeRemaining(game));
            case "time_formatted" -> this.formatTime(this.timeRemaining(game));
            case "elapsed" -> String.valueOf(this.elapsed(game));
            case "elapsed_formatted" -> this.formatTime(this.elapsed(game));
            case "world" -> this.worldName(game);
            case "teams" -> String.valueOf(game.arena.getTeams().size());
            case "teams_playing" -> String.valueOf(this.teamsAlive(game));
            case "team", "team_color", "team_colored", "team_players",
                    "team_max_players", "team_bed", "team_bed_symbol" -> this.teamProp(game, player.getUniqueId(), id);
            default -> this.specificTeam(game, id);
        };
    }

    private String outsideGame(final String id) {
        return switch (id) {
            case "in_game" -> "false";
            case "state" -> "none";
            case "arena", "game_players", "min_players", "game_max_players", "time", "time_formatted",
                    "elapsed", "elapsed_formatted", "world", "teams", "teams_playing",
                    "team", "team_color", "team_colored", "team_players",
                    "team_max_players", "team_bed", "team_bed_symbol" -> "";
            default -> id.startsWith("team_") ? "" : null;
        };
    }

    private String teamProp(final Game game, final UUID playerId, final String id) {
        final GamePlayer gp = game.players.get(playerId);
        if (gp == null) {
            return "";
        }
        final ArenaTeam team = gp.getTeam();
        return switch (id) {
            case "team" -> team.getName();
            case "team_color" -> team.getColor();
            case "team_colored" -> this.colored(team);
            case "team_players" -> String.valueOf(game.teams.get(team).size());
            case "team_max_players" -> String.valueOf(this.slotsPerTeam(game));
            case "team_bed" -> String.valueOf(!game.bedlessTeams.contains(team));
            case "team_bed_symbol" -> game.bedlessTeams.contains(team) ? "" : BED_SYMBOL;
            default -> null;
        };
    }

    @Nullable
    private String specificTeam(final Game game, final String id) {
        final String rest = id.substring("team_".length());
        final String prop = this.matchProp(rest, TEAM_PROPS);
        if (prop == null) {
            return null;
        }
        final ArenaTeam team = this.findTeam(game, rest.substring(0, rest.length() - prop.length() - 1));
        if (team == null) {
            return "";
        }
        return switch (prop) {
            case "name" -> team.getName();
            case "color" -> team.getColor();
            case "colored" -> this.colored(team);
            case "players" -> String.valueOf(game.teams.get(team).size());
            case "max_players" -> String.valueOf(this.slotsPerTeam(game));
            case "alive" -> String.valueOf(!game.eliminatedTeams.contains(team));
            case "bed" -> String.valueOf(!game.bedlessTeams.contains(team));
            case "bed_symbol" -> game.bedlessTeams.contains(team) ? "" : BED_SYMBOL;
            default -> null;
        };
    }

    @Nullable
    private String arena(final Collection<Game> games, final String rest) {
        final String prop = this.matchProp(rest, ARENA_PROPS);
        if (prop == null) {
            return null;
        }
        final String arenaName = rest.substring(0, rest.length() - prop.length() - 1);
        final Arena arena = this.gameManager.getArenaManager().get(arenaName);
        if (arena == null) {
            return "";
        }
        final Game first = this.firstGameOf(games, arenaName);
        return switch (prop) {
            case "name" -> arena.getName();
            case "players" -> String.valueOf(this.arenaPlayers(games, arenaName));
            case "spectators" -> String.valueOf(this.arenaSpectators(games, arenaName));
            case "total" -> String.valueOf(this.arenaPlayers(games, arenaName) + this.arenaSpectators(games, arenaName));
            case "min_players" -> String.valueOf(this.minPlayersFor(arena));
            case "max_players" -> String.valueOf(arena.getTeams().size() * this.slotsForArena(arena));
            case "state" -> first != null ? first.getState().name().toLowerCase(Locale.ROOT) : "none";
            case "world" -> this.worldName(first, arena);
            case "time" -> String.valueOf(first != null ? this.timeRemaining(first) : -1);
            case "time_formatted" -> this.formatTime(first != null ? this.timeRemaining(first) : -1);
            default -> null;
        };
    }

    private String colored(final ArenaTeam team) {
        return this.chatColor(team.getColor()) + team.getName();
    }

    private int minPlayers(final Game game) {
        return this.minPlayersFor(game.arena);
    }

    private int minPlayersFor(final Arena arena) {
        return arena.getMinTeamsToStart() * arena.getMinPlayersPerTeam();
    }

    private int capacity(final Game game) {
        return game.arena.getTeams().size() * this.slotsPerTeam(game);
    }

    private int slotsPerTeam(final Game game) {
        final int arenaMax = game.arena.getMaxPlayersPerTeam();
        if (arenaMax > 0) {
            return arenaMax;
        }
        final ArenaMode mode = game.mode;
        if (mode != null) {
            return mode.getTeamSize();
        }
        return this.largestValidMode(game.arena.getTeams().size()).getTeamSize();
    }

    private int slotsForArena(final Arena arena) {
        final int arenaMax = arena.getMaxPlayersPerTeam();
        if (arenaMax > 0) {
            return arenaMax;
        }
        return this.largestValidMode(arena.getTeams().size()).getTeamSize();
    }

    private ArenaMode largestValidMode(final int teamCount) {
        ArenaMode largest = ArenaMode.SOLO;
        for (final ArenaMode mode : ArenaMode.values()) {
            if (mode.isValidFor(teamCount) && mode.getTeamSize() > largest.getTeamSize()) {
                largest = mode;
            }
        }
        return largest;
    }

    private int elapsed(final Game game) {
        return game.tick / 20;
    }

    private int timeRemaining(final Game game) {
        final int limit = game.arena.getTimeLimit();
        if (limit <= 0) {
            return -1;
        }
        return Math.max(0, limit - this.elapsed(game));
    }

    private String formatTime(final int seconds) {
        if (seconds < 0) {
            return "\u221E";
        }
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }

    private int teamsAlive(final Game game) {
        return game.arena.getTeams().size() - game.eliminatedTeams.size();
    }

    private String worldName(final Game game) {
        final String name = game.arena.getWorldName();
        return name != null ? name : "";
    }

    private String worldName(final Game game, final Arena arena) {
        return game != null ? this.worldName(game) : this.nullableWorld(arena);
    }

    private String nullableWorld(final Arena arena) {
        final String name = arena.getWorldName();
        return name != null ? name : "";
    }

    private boolean matchesMode(final Game game, final ArenaMode mode, final boolean libre) {
        return libre ? game.mode == null : game.mode == mode;
    }

    private int modePlayers(final Collection<Game> games, final ArenaMode mode, final boolean libre) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre)) {
                total += game.players.size();
            }
        }
        return total;
    }

    private int modeSpectators(final Collection<Game> games, final ArenaMode mode, final boolean libre) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre)) {
                total += game.spectators.size();
            }
        }
        return total;
    }

    private int modeGames(final Collection<Game> games, final ArenaMode mode, final boolean libre) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre)) {
                total++;
            }
        }
        return total;
    }

    private int modeGamesInState(final Collection<Game> games, final ArenaMode mode, final boolean libre, final GameState state) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre) && game.getState() == state) {
                total++;
            }
        }
        return total;
    }

    private int modeGamesInLobby(final Collection<Game> games, final ArenaMode mode, final boolean libre) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre) && this.isLobby(game)) {
                total++;
            }
        }
        return total;
    }

    private int modeCapacity(final Collection<Game> games, final ArenaMode mode, final boolean libre) {
        int total = 0;
        for (final Game game : games) {
            if (this.matchesMode(game, mode, libre)) {
                total += this.capacity(game);
            }
        }
        return total;
    }

    private int serverPlayers(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            total += game.players.size();
        }
        return total;
    }

    private int serverSpectators(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            total += game.spectators.size();
        }
        return total;
    }

    private int serverCapacity(final Collection<Game> games) {
        int total = 0;
        for (final Game game : games) {
            total += this.capacity(game);
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
            if (this.isLobby(game)) {
                total++;
            }
        }
        return total;
    }

    private boolean anyInState(final Collection<Game> games, final GameState state) {
        for (final Game game : games) {
            if (game.getState() == state) {
                return true;
            }
        }
        return false;
    }

    private boolean anyInLobby(final Collection<Game> games) {
        for (final Game game : games) {
            if (this.isLobby(game)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLobby(final Game game) {
        final GameState state = game.getState();
        return state == GameState.WAITING || state == GameState.STARTING;
    }

    private int arenaPlayers(final Collection<Game> games, final String arenaName) {
        int total = 0;
        for (final Game game : games) {
            if (game.arena.getName().equalsIgnoreCase(arenaName)) {
                total += game.players.size();
            }
        }
        return total;
    }

    private int arenaSpectators(final Collection<Game> games, final String arenaName) {
        int total = 0;
        for (final Game game : games) {
            if (game.arena.getName().equalsIgnoreCase(arenaName)) {
                total += game.spectators.size();
            }
        }
        return total;
    }

    private @Nullable Game firstGameOf(final Collection<Game> games, final String arenaName) {
        for (final Game game : games) {
            if (game.arena.getName().equalsIgnoreCase(arenaName)) {
                return game;
            }
        }
        return null;
    }

    private @Nullable ArenaTeam findTeam(final Game game, final String teamName) {
        for (final ArenaTeam team : game.arena.getTeams()) {
            if (team.getName().equalsIgnoreCase(teamName)) {
                return team;
            }
        }
        return null;
    }

    private @Nullable String matchProp(final String rest, final String[] props) {
        for (final String prop : props) {
            if (rest.endsWith("_" + prop)) {
                return prop;
            }
        }
        return null;
    }

    private String chatColor(final String colorName) {
        return switch (colorName) {
            case "azul", "BLUE" -> "\u00A79";
            case "vermelho", "RED" -> "\u00A7c";
            case "verde", "GREEN" -> "\u00A7a";
            case "amarelo", "YELLOW" -> "\u00A7e";
            case "roxo", "PURPLE" -> "\u00A75";
            case "rosa", "PINK" -> "\u00A7d";
            case "laranja", "ORANGE" -> "\u00A76";
            case "ciano", "CYAN" -> "\u00A7b";
            default -> "\u00A7f";
        };
    }
}