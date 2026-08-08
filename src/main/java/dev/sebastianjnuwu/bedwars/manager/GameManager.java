package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
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
    private final Set<String> buildingArenas;
    private final Map<String, List<PendingJoin>> pendingJoins;
    private final PlayerStateManager playerStateManager;

    public GameManager(final JavaPlugin plugin, final ArenaManager arenaManager, final ConfigManager configManager, final LangManager lang, final EditorManager editorManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.configManager = configManager;
        this.lang = lang;
        this.games = new HashMap<>();
        this.playerGames = new HashMap<>();
        this.shopNpcManager = new ShopNpcManager(plugin, configManager.getNpcBackend());
        this.editorManager = editorManager;
        this.buildingArenas = new HashSet<>();
        this.pendingJoins = new HashMap<>();
        this.playerStateManager = new PlayerStateManager(plugin, lang);
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
        if (arena.getMinTeamsToStart() > arena.getTeams().size()) {
            missing.add(this.lang.raw("game.validate_min_teams", arena.getName()));
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
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_game"));
                return;
            }
            final Game target = this.findGameByCode(code);
            if (target == null || !target.getArena().getName().equalsIgnoreCase(arenaName)) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.code_not_found", code));
                return;
            }
            if (mode != null && target.getMode() != mode) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.code_mode_mismatch", code));
                return;
            }
            if (target.getState() != GameState.WAITING && target.getState() != GameState.STARTING) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.in_progress"));
                return;
            }
            if (target.isFull()) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.code_full"));
                return;
            }
            target.join(player, teamName, teleport);
            this.playerGames.put(player.getUniqueId(), target);
            return;
        }
        if (this.isInGame(player)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_game"));
            return;
        }
        final Arena arena = this.arenaManager.get(arenaName);
        if (arena == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.arena_not_found", arenaName));
            return;
        }
        if (mode != null && !mode.isValidFor(arena.getTeams().size())) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.mode_not_supported", mode.name().toLowerCase(), arenaName));
            return;
        }
        if (mode != null && arena.getMaxPlayersPerTeam() > 0 && mode.getTeamSize() > arena.getMaxPlayersPerTeam()) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.mode_exceeds_team_limit",
                    mode.name().toLowerCase(), arena.getMaxPlayersPerTeam()));
            return;
        }

        if (this.editorManager.isBeingEdited(arenaName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.arena_being_edited", arenaName));
            return;
        }

        Game game = this.findOpenGame(arenaName, mode);
        if (game == null) {
            final Game active = this.findFirstByArenaName(arenaName);
            if (active != null && active.getState() != GameState.WAITING && active.getState() != GameState.STARTING) {
                active.joinAsSpectator(player);
                this.playerGames.put(player.getUniqueId(), active);
                return;
            }
            this.enqueueJoin(player, arenaName, teamName, mode, teleport);
            return;
        }

        game.join(player, teamName, teleport);
        this.playerGames.put(player.getUniqueId(), game);
    }

    /**
     * Enfileira o jogador para entrar numa arena cujo mundo ainda não existe,
     * iniciando a construção assíncrona da instância quando necessário.
     * <p>
     * Se a arena já estiver sendo construída, o jogador é apenas adicionado à
     * fila de espera. Quando o mundo fica pronto, todos os jogadores pendentes
     * são teleportados para dentro da partida na main thread.
     * </p>
     *
     * @param player    jogador que deseja entrar (não nulo)
     * @param arenaName nome da arena (não nulo)
     * @param teamName  time desejado ou {@code null} para seleção automática
     * @param mode      modo de partida ou {@code null}
     * @param teleport  se deve teleportar o jogador quando o mundo estiver pronto
     */
    private void enqueueJoin(final Player player, final String arenaName, final @Nullable String teamName, final @Nullable ArenaMode mode, final boolean teleport) {
        final String key = queueKey(arenaName, mode);
        final List<PendingJoin> queue = this.pendingJoins.computeIfAbsent(key, k -> new ArrayList<>());
        final UUID playerId = player.getUniqueId();
        if (queue.stream().anyMatch(pending -> pending.playerId().equals(playerId))) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }
        queue.add(new PendingJoin(playerId, teamName, mode, teleport));
        if (this.buildingArenas.contains(key)) {
            player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "game.countdown_preparing"));
            return;
        }
        this.buildingArenas.add(key);
        player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "game.countdown_preparing"));
        this.arenaManager.createInstanceAsync(arenaName, instance -> this.completePendingJoins(arenaName, mode, instance));
    }

    /**
     * Finaliza as entradas pendentes de uma arena após a construção do mundo.
     * <p>
     * Executado na main thread pelo callback da construção assíncrona. Cria a
     * partida a partir da instância pronta e teleporta todos os jogadores que
     * estavam na fila de espera.
     * </p>
     *
     * @param arenaName nome da arena (não nulo)
     * @param instance  instância pronta, ou {@code null} se a construção falhou
     */
    private void completePendingJoins(final String arenaName, final @Nullable ArenaMode mode, final @Nullable Arena instance) {
        final String key = queueKey(arenaName, mode);
        this.buildingArenas.remove(key);
        final List<PendingJoin> waiters = this.pendingJoins.remove(key);
        if (instance == null) {
            if (waiters != null) {
                for (final PendingJoin pending : waiters) {
                    final Player player = Bukkit.getPlayer(pending.playerId());
                    if (player != null) {
                        player.sendMessage(this.lang.text(NamedTextColor.RED, "game.world_not_ready", arenaName));
                    }
                }
            }
            return;
        }
        final List<String> missing = this.validateArena(instance);
        if (!missing.isEmpty()) {
            this.arenaManager.deleteInstanceWorld(instance.getWorldName());
            if (waiters != null) {
                for (final PendingJoin pending : waiters) {
                    final Player player = Bukkit.getPlayer(pending.playerId());
                    if (player == null) {
                        continue;
                    }
                    player.sendMessage(this.lang.text(NamedTextColor.RED, "game.not_ready", arenaName));
                    for (final String msg : missing) {
                        player.sendMessage(this.lang.text(NamedTextColor.GRAY, "game.missing_entry", msg));
                    }
                }
            }
            return;
        }
        final List<PendingJoin> online = waiters != null
                ? waiters.stream().filter(pending -> Bukkit.getPlayer(pending.playerId()) != null).toList()
                : List.of();
        if (online.isEmpty()) {
            this.arenaManager.deleteInstanceWorld(instance.getWorldName());
            return;
        }
        final Game game = new Game(this, instance, this.shopNpcManager, mode);
        this.games.put(this.gameKey(instance), game);
        for (final PendingJoin pending : online) {
            final Player player = Bukkit.getPlayer(pending.playerId());
            if (player != null) {
                game.join(player, pending.teamName(), pending.teleport());
                this.playerGames.put(player.getUniqueId(), game);
            }
        }
    }

    /**
     * Remove o jogador das filas de entrada pendentes (ex.: ao sair do servidor).
     * <p>
     * Não remove a arena em construção; a fila vazia é limpa e, quando a
     * construção terminar, o mundo é descartado por {@link #completePendingJoins}.
     * </p>
     *
     * @param player jogador que deve ser removido das filas (não nulo)
     */
    public void removeFromPendingJoins(final Player player) {
        final UUID playerId = player.getUniqueId();
        for (final List<PendingJoin> queue : this.pendingJoins.values()) {
            queue.removeIf(pending -> pending.playerId().equals(playerId));
        }
        this.pendingJoins.values().removeIf(queue -> queue.isEmpty());
    }

    /**
     * Representa uma entrada pendente aguardando a construção do mundo da arena.
     */
    private record PendingJoin(UUID playerId, @Nullable String teamName, @Nullable ArenaMode mode, boolean teleport) {
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
        Game game = this.findOpenGame(arenaName, null);
        if (game == null) {
            game = this.findFirstByArenaName(arenaName);
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
            if (game.getArena().getName().equalsIgnoreCase(arenaName)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Busca uma partida pelo código público (case-insensitive).
     *
     * @param code código da partida (não nulo)
     * @return a partida correspondente, ou {@code null} se não existir
     */
    private @Nullable Game findGameByCode(final String code) {
        for (final Game game : this.games.values()) {
            if (game.getCode().equalsIgnoreCase(code)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Retorna os códigos das partidas abertas (em lobby/início) de uma arena,
     * usado para autocompletar o argumento {@code --code}.
     *
     * @param arenaName nome da arena (não nulo)
     * @return lista de códigos das partidas abertas (nunca nula)
     */
    public List<String> listOpenCodes(final String arenaName) {
        final List<String> codes = new ArrayList<>();
        for (final Game game : this.games.values()) {
            final Arena arena = game.getArena();
            if (arena.getName().equalsIgnoreCase(arenaName)
                    && (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)
                    && !game.isFull()) {
                codes.add(game.getCode());
            }
        }
        return codes;
    }

    private @Nullable Game findOpenGame(final String arenaName, final @Nullable ArenaMode mode) {
        for (final Game game : this.games.values()) {
            final Arena arena = game.getArena();
            if (arena.getName().equalsIgnoreCase(arenaName)
                    && (mode == null || game.getMode() == mode)
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

    private static String queueKey(final String arenaName, final @Nullable ArenaMode mode) {
        return arenaName + ":" + (mode != null ? mode.name() : "FREE");
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
