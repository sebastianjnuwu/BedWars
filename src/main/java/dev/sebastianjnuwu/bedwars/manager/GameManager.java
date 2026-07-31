package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        return this.games.get(arenaName);
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

        if (!this.arenaManager.ensureArenaReady(arena)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.world_not_ready", arenaName));
            return;
        }

        final Arena refreshedArena = this.arenaManager.get(arenaName);

        final List<String> missing = this.validateArena(refreshedArena);
        if (!missing.isEmpty()) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.not_ready", arenaName));
            for (final String msg : missing) {
                player.sendMessage(this.lang.text(NamedTextColor.GRAY, "game.missing_entry", msg));
            }
            return;
        }

        Game game = this.games.get(arenaName);
        if (game == null) {
            game = new Game(this, refreshedArena, getShopNpcManager());
            this.games.put(arenaName, game);
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
                this.shopNpcManager.removeGameNpcs(game.getArena().getName());
                this.games.remove(game.getArena().getName());
                this.debug("debug.room_closed_empty", game.getArena().getName());
            }
        }
    }

    @Override
    public void startGame(final String arenaName) {
        Game game = this.games.get(arenaName);
        if (game == null) {
            final Arena arena = this.arenaManager.get(arenaName);
            if (arena == null) {
                return;
            }
            if (!this.arenaManager.ensureArenaReady(arena)) {
                return;
            }
            final Arena refreshed = this.arenaManager.get(arenaName);
            if (refreshed == null) {
                return;
            }
            game = new Game(this, refreshed, this.shopNpcManager);
            this.games.put(arenaName, game);
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
    public void removeGame(final String arenaName) {
        final Game game = this.games.remove(arenaName);
        if (game != null) {
            this.shopNpcManager.removeGameNpcs(arenaName);
            this.playerGames.values().removeIf(g -> g == game);
            this.debug("debug.room_closed", arenaName);
        }
    }

    private void debug(final String key, final Object... args) {
        if (this.configManager.isDebugEnabled()) {
            this.plugin.getLogger().info("[BedWars] " + this.lang.raw(key, args));
        }
    }

    public void forceEndAll() {
        for (final Game game : this.games.values()) {
            game.forceEnd();
        }
        this.games.clear();
        this.playerGames.clear();
        this.shopNpcManager.removeAll();
    }
}
