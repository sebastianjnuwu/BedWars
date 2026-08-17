package dev.sebastianjnuwu.bedwars.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.combat.GameCombat;
import dev.sebastianjnuwu.bedwars.game.ending.GameEnding;
import dev.sebastianjnuwu.bedwars.game.lifecycle.GameLifecycle;
import dev.sebastianjnuwu.bedwars.game.ticker.GameTicker;
import dev.sebastianjnuwu.bedwars.game.upgrade.GameUpgrades;
import dev.sebastianjnuwu.bedwars.game.util.GameCodeGenerator;
import dev.sebastianjnuwu.bedwars.game.util.GameDebug;
import dev.sebastianjnuwu.bedwars.game.util.GameQueries;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;

/**
 * Representa uma única instância de partida de BedWars. Gerencia a máquina de estados,
 * entrada/saída de jogadores, mortes/renascimentos, quebra de berços, eliminação de equipes,
 * condição de vitória e contagem regressiva de início automático.
 * <p>
 * A implementação é delegada a um conjunto de gerenciadores internos (composição):
 * {@link ChatManager}, {@link GameItems}, {@link GameLifecycle}, {@link GameUpgrades},
 * {@link GameTicker}, {@link GameCombat} e {@link GameEnding}.
 * </p>
 */
public class Game implements dev.sebastianjnuwu.bedwars.api.model.Game {

    public final GameManager gameManager;
    public final ShopNpcManager shopNpcManager;
    public final ChatManager chat;
    private final GameItems items;
    private final GameLifecycle lifecycle;
    private final GameUpgrades upgrades;
    private final GameTicker ticker;
    private final GameCombat combat;
    private final GameEnding ending;
    private final GameQueries queries;
    public final LangManager lang;
    public final Arena arena;
    public final ArenaMode mode;
    public final String code;
    public final Map<ArenaTeam, List<UUID>> teams;
    public final Map<UUID, GamePlayer> players;
    public final Set<ArenaTeam> eliminatedTeams;
    public final Set<ArenaTeam> bedlessTeams;
    public final Set<UUID> spectators;
    public final Map<ArenaGenerator, Integer> forgeLevels;
    public final Map<ArenaGenerator, long[]> generatorTicks;
    public final Map<String, long[]> forgeTicks;
    public final Map<ArenaTeam, Integer> sharpnessLevels;
    public final Map<ArenaTeam, Integer> protectionLevels;
    public final Map<UUID, Integer> respawnTicks;
    public final Set<UUID> pendingFinalRespawns;
    public final Set<String> placedBlocks;
    public GameState state;
    public BukkitTask gameTickTask;
    public int tick;
    public int countdownSeconds;
    public int timeLimitWarning;
    public final Set<Integer> timeLimitWarningsSent = new HashSet<>();

    /**
     * Constrói uma nova instância de partida para a arena informada.
     * <p>
     * Inicializa todos os mapas internos e define o estado inicial como
     * {@link GameState#WAITING}. Os times são populados a partir da configuração
     * da arena — cada time começa com a lista de jogadores vazia. O modo define
     * quantos jogadores cabem por time; quando {@code null}, a capacidade é
     * derivada do mínimo de jogadores da arena.
     * </p>
     *
     * @param gameManager gerenciador de partidas que controla esta instância (não nulo)
     * @param arena       configuração da arena onde a partida será realizada (não nula)
     * @param mode        modo de partida (solo/dupla/trio/quarteto) ou null para livre
     */
    public Game(final GameManager gameManager, final Arena arena, final ShopNpcManager shopNpcManager, final @Nullable ArenaMode mode) {
        this.gameManager = gameManager;
        this.shopNpcManager = shopNpcManager;
        this.lang = gameManager.getLang();
        this.chat = new ChatManager(this);
        this.items = new GameItems(this, this.lang);
        this.lifecycle = new GameLifecycle(this);
        this.upgrades = new GameUpgrades(this);
        this.ticker = new GameTicker(this);
        this.combat = new GameCombat(this);
        this.ending = new GameEnding(this);
        this.queries = new GameQueries(this);
        this.arena = arena;
        this.mode = mode;
        this.code = GameCodeGenerator.generate();
        this.teams = new HashMap<>();
        this.players = new HashMap<>();
        this.eliminatedTeams = new HashSet<>();
        this.bedlessTeams = new HashSet<>();
        this.spectators = new HashSet<>();
        this.forgeLevels = new HashMap<>();
        this.generatorTicks = new HashMap<>();
        this.forgeTicks = new HashMap<>();
        this.sharpnessLevels = new HashMap<>();
        this.protectionLevels = new HashMap<>();
        this.respawnTicks = new HashMap<>();
        this.pendingFinalRespawns = new HashSet<>();
        this.placedBlocks = new HashSet<>();
        this.state = GameState.WAITING;
        for (final ArenaTeam team : arena.getTeams()) {
            this.teams.put(team, new ArrayList<>());
        }
    }

    /**
     * Retorna a arena associada a esta partida.
     *
     * @return a arena (não nula)
     */
    public Arena getArena() {
        return this.arena;
    }

    /**
     * Retorna o código público da partida (6 caracteres, ex.: {@code ABC123}),
     * usado para entrar numa sala específica.
     *
     * @return o código da partida (não nulo)
     */
    public String getCode() {
        return this.code;
    }

    public GameState getState() {
        return this.state;
    }

    /**
     * Registra uma mensagem de depuração quando o debug está ativo.
     *
     * @param key  chave da mensagem no arquivo de língua
     * @param args argumentos de formatação da mensagem
     */
    public void debug(final String key, final Object... args) {
        GameDebug.log(this, key, args);
    }

    public boolean isSpectator(final Player player) {
        return this.queries.isSpectator(player);
    }

    public void joinAsSpectator(final Player player) {
        this.lifecycle.joinAsSpectator(player);
    }

    public void becomeSpectator(final Player player) {
        this.lifecycle.becomeSpectator(player);
    }

    public boolean isBedless(final ArenaTeam team) {
        return this.queries.isBedless(team);
    }

    public boolean isEliminated(final ArenaTeam team) {
        return this.queries.isEliminated(team);
    }

    public @Nullable ArenaTeam getPlayerTeam(final Player player) {
        return this.queries.getPlayerTeam(player);
    }

    public @Nullable GamePlayer getGamePlayer(final Player player) {
        return this.queries.getGamePlayer(player);
    }

    public boolean isPlaying(final Player player) {
        return this.queries.isPlaying(player);
    }

    public void trackPlacedBlock(final Location location) {
        this.queries.trackPlacedBlock(location);
    }

    public boolean isPlacedBlock(final Location location) {
        return this.queries.isPlacedBlock(location);
    }

    public void join(final Player player) {
        this.lifecycle.join(player);
    }

    public void join(final Player player, final @Nullable String teamName) {
        this.lifecycle.join(player, teamName);
    }

    public void join(final Player player, final @Nullable String teamName, final boolean teleport) {
        this.lifecycle.join(player, teamName, teleport);
    }

    public void switchTeam(final Player player, final String teamName) {
        this.lifecycle.switchTeam(player, teamName);
    }

    public void leave(final Player player) {
        this.lifecycle.leave(player);
    }

    public void start() {
        this.ticker.start();
    }

    /**
     * Inicia a partida imediatamente, ignorando a exigência de jogadores em
     * pelo menos 2 times (usado pelo comando de admin /bw start).
     */
    public void forceStart() {
        this.ticker.forceStart();
    }

    public int getForgeLevel(final ArenaGenerator forge) {
        return this.upgrades.getForgeLevel(forge);
    }

    public @Nullable ForgeLevel getForgeUpgradeLevel(final ArenaGenerator forge) {
        return this.upgrades.getForgeUpgradeLevel(forge);
    }

    public boolean upgradeForge(final ArenaGenerator forge) {
        return this.upgrades.upgradeForge(forge);
    }

    public int getSharpnessLevel(final ArenaTeam team) {
        return this.upgrades.getSharpnessLevel(team);
    }

    public boolean upgradeSharpness(final ArenaTeam team) {
        return this.upgrades.upgradeSharpness(team);
    }

    public int getProtectionLevel(final ArenaTeam team) {
        return this.upgrades.getProtectionLevel(team);
    }

    public boolean upgradeProtection(final ArenaTeam team) {
        return this.upgrades.upgradeProtection(team);
    }

    public int getMaxUpgradeLevel() {
        return this.upgrades.getMaxUpgradeLevel();
    }

    public @Nullable ForgeLevel getSharpnessUpgradeLevel(final ArenaTeam team) {
        return this.upgrades.getSharpnessUpgradeLevel(team);
    }

    public @Nullable ForgeLevel getProtectionUpgradeLevel(final ArenaTeam team) {
        return this.upgrades.getProtectionUpgradeLevel(team);
    }

    public void killPlayer(final Player player) {
        this.combat.killPlayer(player);
    }

    public void breakBed(final ArenaTeam team) {
        this.combat.breakBed(team);
    }

    public int getPlayerCount() {
        return this.queries.getPlayerCount();
    }

    public boolean isFull() {
        return this.queries.isFull();
    }

    public Map<ArenaTeam, List<UUID>> getTeams() {
        return this.teams;
    }

    @Override
    public Collection<GamePlayer> getGamePlayers() {
        return this.queries.getGamePlayers();
    }

    public Collection<Player> getPlayers() {
        return this.queries.getPlayers();
    }

    public Collection<Player> getSpectatorPlayers() {
        return this.queries.getSpectatorPlayers();
    }

    public void broadcast(final String message) {
        this.queries.broadcast(message);
    }

    public void forceEnd() {
        this.ending.forceEnd();
    }

    /**
     * Retorna o modo de partida desta instância, ou {@code null} para partidas
     * livres (capacidade derivada do mínimo de jogadores da arena).
     *
     * @return modo da partida ou null
     */
    public @Nullable ArenaMode getMode() {
        return this.mode;
    }

    public GameItems items() {
        return this.items;
    }

    public GameLifecycle lifecycle() {
        return this.lifecycle;
    }

    public GameUpgrades upgrades() {
        return this.upgrades;
    }

    public GameTicker ticker() {
        return this.ticker;
    }

    public GameCombat combat() {
        return this.combat;
    }

    public GameEnding ending() {
        return this.ending;
    }
}
