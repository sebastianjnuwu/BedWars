package dev.sebastianjnuwu.bedwars.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
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

    private static final MiniMessage MM = MiniMessage.miniMessage();

    final GameManager gameManager;
    final ShopNpcManager shopNpcManager;
    final ChatManager chat;
    private final GameItems items;
    private final GameLifecycle lifecycle;
    private final GameUpgrades upgrades;
    private final GameTicker ticker;
    private final GameCombat combat;
    private final GameEnding ending;
    final LangManager lang;
    final Arena arena;
    final ArenaMode mode;
    final String code;
    final Map<ArenaTeam, List<UUID>> teams;
    final Map<UUID, GamePlayer> players;
    final Set<ArenaTeam> eliminatedTeams;
    final Set<ArenaTeam> bedlessTeams;
    final Set<UUID> spectators;
    final Map<ArenaGenerator, Integer> forgeLevels;
    final Map<ArenaGenerator, long[]> generatorTicks;
    final Map<String, long[]> forgeTicks;
    final Map<ArenaTeam, Integer> sharpnessLevels;
    final Map<ArenaTeam, Integer> protectionLevels;
    final Map<UUID, Integer> respawnTicks;
    final Set<UUID> pendingFinalRespawns;
    final Set<String> placedBlocks;
    GameState state;
    BukkitTask gameTickTask;
    int tick;
    int countdownSeconds;
    int timeLimitWarning;
    final Set<Integer> timeLimitWarningsSent = new HashSet<>();

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
        this.arena = arena;
        this.mode = mode;
        this.code = generateCode();
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

    /**
     * Gera um código aleatório de 6 caracteres alfanuméricos em maiúsculas.
     */
    private static String generateCode() {
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        final StringBuilder sb = new StringBuilder(6);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
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
    void debug(final String key, final Object... args) {
        if (this.gameManager.getConfigManager().isDebugEnabled()) {
            Bukkit.getLogger().info("[BedWars] " + this.lang.raw(key, args));
        }
    }

    public boolean isSpectator(final Player player) {
        return this.spectators.contains(player.getUniqueId());
    }

    public void joinAsSpectator(final Player player) {
        this.lifecycle.joinAsSpectator(player);
    }

    public void becomeSpectator(final Player player) {
        this.lifecycle.becomeSpectator(player);
    }

    public boolean isBedless(final ArenaTeam team) {
        return this.bedlessTeams.contains(team);
    }

    public boolean isEliminated(final ArenaTeam team) {
        return this.eliminatedTeams.contains(team);
    }

    public @Nullable ArenaTeam getPlayerTeam(final Player player) {
        final GamePlayer gp = this.players.get(player.getUniqueId());
        return gp != null ? gp.getTeam() : null;
    }

    public @Nullable GamePlayer getGamePlayer(final Player player) {
        return this.players.get(player.getUniqueId());
    }

    public boolean isPlaying(final Player player) {
        final GamePlayer gp = this.players.get(player.getUniqueId());
        return gp != null && gp.isAlive() && this.state == GameState.PLAYING;
    }

    public void trackPlacedBlock(final Location location) {
        this.placedBlocks.add(blockKey(location));
    }

    public boolean isPlacedBlock(final Location location) {
        return this.placedBlocks.contains(blockKey(location));
    }

    private static String blockKey(final Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX()
                + ":" + location.getBlockY() + ":" + location.getBlockZ();
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
        return this.players.size();
    }

    public boolean isFull() {
        final int capacity = this.arena.getTeams().size() * this.lifecycle.maxTeamSlots();
        return this.players.size() >= capacity;
    }

    public Map<ArenaTeam, List<UUID>> getTeams() {
        return this.teams;
    }

    @Override
    public Collection<GamePlayer> getGamePlayers() {
        return this.players.values();
    }

    public Collection<Player> getPlayers() {
        return this.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Player> getSpectatorPlayers() {
        return this.spectators.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void broadcast(final String message) {
        final Component component = MM.deserialize(message);
        this.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> CompatProvider.chat().sendMessage(p, component));
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

    /**
     * Retorna o gerenciador de itens desta partida.
     *
     * @return gerenciador de itens (não nulo)
     */
    GameItems items() {
        return this.items;
    }

    /**
     * Retorna o gerenciador de ciclo de vida dos jogadores.
     *
     * @return gerenciador de ciclo de vida (não nulo)
     */
    GameLifecycle lifecycle() {
        return this.lifecycle;
    }

    /**
     * Retorna o gerenciador de upgrades da partida.
     *
     * @return gerenciador de upgrades (não nulo)
     */
    GameUpgrades upgrades() {
        return this.upgrades;
    }

    /**
     * Retorna o gerenciador de ticks da partida.
     *
     * @return gerenciador de ticks (não nulo)
     */
    GameTicker ticker() {
        return this.ticker;
    }

    /**
     * Retorna o gerenciador de combate da partida.
     *
     * @return gerenciador de combate (não nulo)
     */
    GameCombat combat() {
        return this.combat;
    }

    /**
     * Retorna o gerenciador de encerramento da partida.
     *
     * @return gerenciador de encerramento (não nulo)
     */
    GameEnding ending() {
        return this.ending;
    }
}