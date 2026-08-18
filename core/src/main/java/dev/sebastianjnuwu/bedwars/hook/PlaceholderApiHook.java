package dev.sebastianjnuwu.bedwars.hook;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderApiHook extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PlaceholderData data;

    public PlaceholderApiHook(final JavaPlugin plugin, final GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.data = new PlaceholderData(gameManager);
    }

    @Override
    public String getIdentifier() {
        return "sbedwars";
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
                "players", "spectators", "total_players",
                "games", "games_playing", "games_waiting", "games_ending",
                "max_players", "any_playing", "any_waiting",
                "in_game", "arena", "state", "game_players",
                "min_players", "game_max_players",
                "time", "time_formatted", "elapsed", "elapsed_formatted",
                "world", "teams", "teams_playing",
                "team", "team_color", "team_colored", "team_players",
                "team_max_players", "team_bed", "team_bed_symbol",
                "team_<time>_name", "team_<time>_color", "team_<time>_colored",
                "team_<time>_players", "team_<time>_max_players",
                "team_<time>_alive", "team_<time>_bed", "team_<time>_bed_symbol",
                "arena_<arena>_name", "arena_<arena>_players", "arena_<arena>_spectators",
                "arena_<arena>_total", "arena_<arena>_min_players", "arena_<arena>_max_players",
                "arena_<arena>_state", "arena_<arena>_world",
                "arena_<arena>_time", "arena_<arena>_time_formatted"
        );
    }

    @Override
    public @Nullable String onRequest(final OfflinePlayer player, final String params) {
        if (params == null) {
            return null;
        }
        final String id = params.toLowerCase(Locale.ROOT);
        final Collection<Game> games = this.gameManager.getActiveGames();
        final String serverValue = this.data.server(id, games);
        if (serverValue != null) {
            return serverValue;
        }
        return this.data.playerOrArena(player, id, games);
    }
}