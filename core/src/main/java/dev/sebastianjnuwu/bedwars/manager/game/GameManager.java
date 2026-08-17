package dev.sebastianjnuwu.bedwars.manager.game;

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
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.PlayerStateManager;
import dev.sebastianjnuwu.bedwars.manager.arena.ArenaManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;

/**
 * Gerencia todas as partidas ativas de BedWars e o mapeamento de jogadores para partidas.
 * Lida com criação, entrada, saída e limpeza de partidas.
 */
public class GameManager implements dev.sebastianjnuwu.bedwars.api.GameManager {

    private final JavaPlugin plugin;
    final ArenaManager arenaManager;
    private final ConfigManager configManager;
    final LangManager lang;
    final Map<String, Game> games;
    final Map<UUID, Game> playerGames;
    final ShopNpcManager shopNpcManager;
    private final EditorManager editorManager;
    private final PlayerStateManager playerStateManager;
    private final GameJoinQueue joinQueue;
    private final GameLookup lookup;
    private final GameValidator validator;

    public GameManager(final JavaPlugin plugin, final ArenaManager arenaManager, final ConfigManager configManager, final LangManager lang, final EditorManager editorManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.lang = lang;
        this.games = new HashMap<>();
        this.playerGames = new HashMap<>();
        this.shopNpcManager = new ShopNpcManager(plugin, configManager.getNpcBackend());
        this.editorManager = editorManager;
        this.playerStateManager = new PlayerStateManager(plugin, lang);
        this.joinQueue = new GameJoinQueue(this);
        this.lookup = new GameLookup(this);
        this.validator = new GameValidator(this);
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

    public PlayerStateManager getPlayerStateManager() {
        return this.playerStateManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    @Override
    public @Nullable dev.sebastianjnuwu.bedwars.api.model.Game getGame(final String arenaName) {
        return this.lookup.findFirstByArenaName(arenaName);
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
        return this.validator.validate(arena);
    }

    @Override
    public void joinGame(final Player player, final String arenaName) {
        this.joinGame(player, arenaName, null, null, true);
    }

    @Override
    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName) {
        this.joinGame(player, arenaName, teamName, null, true);
    }

    @Override
    public void joinGame(final Player player, final String arenaName, final @Nullable ArenaMode mode) {
        this.joinGame(player, arenaName, null, mode, true);
    }

    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName, final boolean teleport) {
        this.joinGame(player, arenaName, teamName, null, teleport);
    }

    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName, final @Nullable ArenaMode mode, final boolean teleport) {
        this.joinGame(player, arenaName, teamName, mode, null, teleport);
    }

    public void joinGame(final Player player, final String arenaName, final @Nullable String teamName, final @Nullable ArenaMode mode, final @Nullable String code, final boolean teleport) {
        if (code != null && !code.isBlank()) {
            if (this.isInGame(player)) {
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.already_in_game"));
                return;
            }
            final Game target = this.lookup.findGameByCode(code);
            if (target == null || !target.getArena().getName().equalsIgnoreCase(arenaName)) {
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.code_not_found", code));
                return;
            }
            if (mode != null && target.getMode() != mode) {
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.code_mode_mismatch", code));
                return;
            }
            if (target.getState() != GameState.WAITING && target.getState() != GameState.STARTING) {
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.in_progress"));
                return;
            }
            if (target.isFull()) {
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.code_full"));
                return;
            }
            target.join(player, teamName, teleport);
            this.playerGames.put(player.getUniqueId(), target);
            return;
        }
        if (this.isInGame(player)) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.already_in_game"));
            return;
        }
        final Arena arena = this.arenaManager.get(arenaName);
        if (arena == null) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.arena_not_found", arenaName));
            return;
        }
        if (mode != null && !mode.isValidFor(arena.getTeams().size())) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.mode_not_supported", mode.name().toLowerCase(), arenaName));
            return;
        }
        if (mode != null && arena.getMaxPlayersPerTeam() > 0 && mode.getTeamSize() > arena.getMaxPlayersPerTeam()) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.mode_exceeds_team_limit",
                    mode.name().toLowerCase(), arena.getMaxPlayersPerTeam()));
            return;
        }

        if (this.editorManager.isBeingEdited(arenaName)) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.arena_being_edited", arenaName));
            return;
        }

        Game game = this.lookup.findOpenGame(arenaName, mode);
        if (game == null) {
            final Game active = this.lookup.findFirstByArenaName(arenaName);
            if (active != null && active.getState() != GameState.WAITING && active.getState() != GameState.STARTING) {
                active.joinAsSpectator(player);
                this.playerGames.put(player.getUniqueId(), active);
                return;
            }
            this.joinQueue.enqueueJoin(player, arenaName, teamName, mode, teleport);
            return;
        }

        game.join(player, teamName, teleport);
        this.playerGames.put(player.getUniqueId(), game);
    }

    /**
     * Remove o jogador das filas de entrada pendentes (ex.: ao sair do servidor).
     *
     * @param player jogador que deve ser removido das filas (não nulo)
     */
    public void removeFromPendingJoins(final Player player) {
        this.joinQueue.removeFromPendingJoins(player);
    }

    public void cleanupPlayer(final Player player) {
        final Game game = (Game) this.playerGames.remove(player.getUniqueId());
        if (game != null) {
            game.leave(player);
            if (game.getPlayers().isEmpty()) {
                this.removeGame(this.gameKey(game.getArena()));
                this.debug("debug.room_closed_empty", game.getArena().getName());
            }
        }
        this.playerStateManager.restorePlayerState(player);
        this.plugin.getLogger().info(this.lang.raw("debug.player_state_cleanup", player.getUniqueId().toString()));
    }

    @Override
    public void leaveGame(final Player player) {
        this.cleanupPlayer(player);
    }

    @Override
    public void startGame(final String arenaName) {
        this.startGame(arenaName, false);
    }

    private void startGame(final String arenaName, final boolean force) {
        Game game = this.lookup.findOpenGame(arenaName, null);
        if (game == null) {
            game = this.lookup.findFirstByArenaName(arenaName);
        }
        if (game == null || (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING)) {
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
            game = new Game(this, instance, this.shopNpcManager, null);
            this.games.put(this.gameKey(instance), game);
        }
        final List<String> missing = this.validateArena(game.getArena());
        if (!missing.isEmpty()) {
            return;
        }
        if (force) {
            game.forceStart();
        } else {
            game.start();
        }
    }

    /**
     * Inicia a partida imediatamente, ignorando a exigência de jogadores em
     * pelo menos 2 times (comando de admin {@code /bw start}).
     *
     * @param arenaName nome da arena (não nulo)
     */
    public void forceStartGame(final String arenaName) {
        this.startGame(arenaName, true);
    }

    @Override
    public void removePlayerMapping(final Player player) {
        this.playerGames.remove(player.getUniqueId());
    }

    @Override
    public void removeGame(final String key) {
        Game game = this.games.get(key);
        if (game == null) {
            game = this.lookup.findFirstByArenaName(key);
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

    /**
     * Retorna os códigos das partidas abertas (em lobby/início) de uma arena,
     * usado para autocompletar o argumento {@code --code}.
     *
     * @param arenaName nome da arena (não nulo)
     * @return lista de códigos das partidas abertas (nunca nula)
     */
    public List<String> listOpenCodes(final String arenaName) {
        return this.lookup.listOpenCodes(arenaName);
    }

    String gameKey(final Arena arena) {
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
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (this.playerStateManager.hasSavedState(player)) {
                this.playerStateManager.restorePlayerState(player);
            }
        }
        this.games.clear();
        this.playerGames.clear();
        this.shopNpcManager.removeAll();
    }
}
