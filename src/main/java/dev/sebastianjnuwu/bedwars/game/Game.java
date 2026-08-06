package dev.sebastianjnuwu.bedwars.game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.events.BedBreakEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameEndEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerDeathEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerEliminateEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStatChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameStartEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameStateChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorSpawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorUpgradeEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerJoinGameEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerKillEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerLeaveGameEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerRespawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.TeamEliminateEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.DeathCause;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.manager.PlayerStateManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Representa uma única instância de partida de BedWars. Gerencia a máquina de estados,
 * entrada/saída de jogadores, mortes/renascimentos, quebra de berços, eliminação de equipes,
 * condição de vitória e contagem regressiva de início automático.
 */
public class Game implements dev.sebastianjnuwu.bedwars.api.model.Game {


    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final GameManager gameManager;
    private final ShopNpcManager shopNpcManager;
    private final LangManager lang;
    private final Arena arena;
    private final ArenaMode mode;
    private final String code;
    private final Map<ArenaTeam, List<UUID>> teams;
    private final Map<UUID, GamePlayer> players;
    private final Set<ArenaTeam> eliminatedTeams;
    private final Set<ArenaTeam> bedlessTeams;
    private final Set<UUID> spectators;
    private final Map<ArenaGenerator, Integer> forgeLevels;
    private final Map<ArenaGenerator, long[]> generatorTicks;
    private final Map<String, long[]> forgeTicks;
    private final Map<UUID, Integer> respawnTicks;
    private final Set<String> placedBlocks;
    private GameState state;
    private BukkitTask gameTickTask;
    private int tick;
    private int countdownSeconds;

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
        this.respawnTicks = new HashMap<>();
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

    public boolean isSpectator(final Player player) {
        return this.spectators.contains(player.getUniqueId());
    }

    public void joinAsSpectator(final Player player) {
        if (this.players.containsKey(player.getUniqueId())) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }
        if (this.spectators.contains(player.getUniqueId())) {
            return;
        }

        this.debug("debug.player_spectator", player.getName(), this.arena.getName());
        this.spectators.add(player.getUniqueId());
        this.saveInventory(player);

        player.sendMessage(MM.deserialize(this.lang.raw("game.game_code", this.code)));

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().setItem(8, createExitDoorItem());

        final Location spawn = this.arena.getArenaSpawn();
        if (spawn != null) {
            LocationUtil.safeTeleport(player, spawn);
        }
    }

    private void debug(final String key, final Object... args) {
        if (this.gameManager.getConfigManager().isDebugEnabled()) {
            Bukkit.getLogger().info("[BedWars] " + this.lang.raw(key, args));
        }
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
        this.join(player, null, true);
    }

    public void join(final Player player, final @Nullable String teamName) {
        this.join(player, teamName, true);
    }

    public void join(final Player player, final @Nullable String teamName, final boolean teleport) {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.in_progress"));
            return;
        }
        if (this.players.containsKey(player.getUniqueId())) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }

        final ArenaTeam team;
        if (teamName != null) {
            team = this.findNamedTeam(teamName);
            if (team == null) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.team_not_found", teamName));
                return;
            }
            if (this.teams.get(team).size() >= this.maxTeamSlots()) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.team_full"));
                return;
            }
        } else {
            team = this.findSmallestTeam();
            if (team == null) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_teams_available"));
                return;
            }
        }

        final var gp = new dev.sebastianjnuwu.bedwars.model.GamePlayer(player.getUniqueId(), team);
        this.players.put(player.getUniqueId(), gp);
        this.teams.get(team).add(player.getUniqueId());

        this.debug("debug.player_joined", player.getName(), this.arena.getName(),
                team.getName(), this.players.size());

        this.saveInventory(player);

        Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, player));

        player.sendMessage(MM.deserialize(this.lang.raw("game.game_code", this.code)));

        if (teleport) {
            final Location spawn = this.arena.getArenaSpawn();
            if (spawn != null) {
                LocationUtil.safeTeleport(player, spawn);
            }
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().setItem(8, createExitDoorItem());
        player.getInventory().setItem(0, createTeamSelectorItem(team));
        applyTeamArmor(player, team);

        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (online == player) {
                continue;
            }
            if (this == this.gameManager.getPlayerGame(online)) {
                player.showPlayer(this.gameManager.getPlugin(), online);
                online.showPlayer(this.gameManager.getPlugin(), player);
                continue;
            }
            player.hidePlayer(this.gameManager.getPlugin(), online);
            online.hidePlayer(this.gameManager.getPlugin(), player);
        }

        final int count = this.players.size();
        final int max = this.arena.getTeams().size();
        final Component msg = this.lang.text(NamedTextColor.GREEN, "game.join_broadcast",
                player.getName(), String.valueOf(count), String.valueOf(max));
        for (final var entry : this.players.entrySet()) {
            final Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.sendMessage(msg);
            }
        }

        this.updateCountdownState();
    }

    public void switchTeam(final Player player, final String teamName) {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.in_progress"));
            return;
        }
        final GamePlayer gp = this.players.get(player.getUniqueId());
        if (gp == null) {
            this.join(player, teamName, false);
            return;
        }
        final ArenaTeam oldTeam = gp.getTeam();
        if (oldTeam.getName().equalsIgnoreCase(teamName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.already_in_team"));
            return;
        }
        final ArenaTeam newTeam = this.findNamedTeam(teamName);
        if (newTeam == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.team_not_found", teamName));
            return;
        }
        if (this.teams.get(newTeam).size() >= this.maxTeamSlots()) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.team_full"));
            return;
        }
        this.teams.get(oldTeam).remove(player.getUniqueId());
        this.teams.get(newTeam).add(player.getUniqueId());
        gp.setTeam(newTeam);
        applyTeamArmor(player, newTeam);
        player.getInventory().setItem(0, createTeamSelectorItem(newTeam));
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "game.switched_team", newTeam.getName()));
        this.updateCountdownState();
    }

    public void leave(final Player player) {
        if (this.state == GameState.ENDING) {
            restoreInventory(player);
            this.gameManager.removePlayerMapping(player);
            return;
        }

        final boolean wasSpectator = this.spectators.remove(player.getUniqueId());
        if (wasSpectator) {
            final Location lobby = this.gameManager.getConfigManager().getLobby();
            if (lobby != null) {
                player.teleport(lobby);
            } else if (!Bukkit.getWorlds().isEmpty()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            // Restaura inventario do mundo normal
            restoreInventory(player);
            return;
        }

        this.respawnTicks.remove(player.getUniqueId());

        final GamePlayer gp = this.players.remove(player.getUniqueId());
        if (gp == null) {
            return;
        }

        final ArenaTeam team = gp.getTeam();
        this.teams.get(team).remove(player.getUniqueId());

        this.debug("debug.player_left", player.getName(), this.arena.getName(),
                this.players.size());

        // Restaura inventario do mundo normal
        restoreInventory(player);

        final Location lobby = this.gameManager.getConfigManager().getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
        player.setGameMode(GameMode.SURVIVAL);

        // Mostra jogadores de outras partidas novamente
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this == this.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.showPlayer(this.gameManager.getPlugin(), online);
            online.showPlayer(this.gameManager.getPlugin(), player);
        }

        final Component msg = this.lang.text(NamedTextColor.YELLOW, "game.leave_broadcast", player.getName());
        // Envia mensagem apenas para jogadores desta partida
        for (final var entry : this.players.entrySet()) {
            final Player p = Bukkit.getPlayer(entry.getKey());
            if (p != null) {
                p.sendMessage(msg);
            }
        }

        Bukkit.getPluginManager().callEvent(new PlayerLeaveGameEvent(this, player));

        this.updateCountdownState();

        if (this.bedlessTeams.contains(team) && this.getAliveCount(team) == 0) {
            this.eliminateTeam(team);
        } else {
            this.checkWinCondition();
        }
    }

    public void start() {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            return;
        }
        if (!this.hasAtLeastTwoTeams()) {
            this.cancelCountdown();
            return;
        }
        this.startPlaying();
    }

    /**
     * Inicia a partida imediatamente, ignorando a exigência de jogadores em
     * pelo menos 2 times (usado pelo comando de admin /bw start).
     */
    public void forceStart() {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            return;
        }
        this.startPlaying();
    }

    private void startPlaying() {
        final GameState prevState = this.state;
        this.state = GameState.PLAYING;
        this.tick = 0;
        this.gameManager.getArenaManager().markWorldDirty(
                this.arena.getWorldName() != null ? this.arena.getWorldName() : "bw_" + this.arena.getName());
        this.initGeneratorTicks();
        this.initForgeTicks();
        this.startGameTick();
        this.debug("debug.game_started", this.arena.getName(), this.players.size());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, prevState, GameState.PLAYING));

        this.restoreArenaSpawnBlock();
        this.restoreTeamSpawnBlocks();

        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            final Location spawnLoc = team.getSpawn();
            if (spawnLoc == null) {
                continue;
            }
            int index = 0;
            for (final UUID uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                final Location spawn = spawnLoc.clone().add(index * 0.5, 0, 0);
                LocationUtil.safeTeleport(player, spawn);
                player.getInventory().clear();
                applyTeamArmor(player, team);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setExp(0);
                player.setLevel(0);
                final GamePlayer gp = this.players.get(uuid);
                if (gp != null) {
                    gp.setAlive(true);
                }
                index++;
            }
        }

        final Title startTitle = Title.title(
                Component.text(this.lang.raw("game.start_title")),
                Component.text(this.lang.raw("game.start_subtitle")),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(4), Duration.ofSeconds(1))
        );
        final Component msg = this.lang.text(NamedTextColor.GOLD, "game.started");
        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (this.players.containsKey(p.getUniqueId())) {
                p.showTitle(startTitle);
                p.sendMessage(msg);
            }
        }

        Bukkit.getPluginManager().callEvent(new GameStartEvent(this));

        this.shopNpcManager.spawnGameNpcs(
                this.arena.getWorldName(),
                this.arena.getShopNpcs()
        );
    }

    private void restoreArenaSpawnBlock() {
        if (this.arena.getArenaSpawn() == null || this.arena.getSpawnBlockData() == null) {
            return;
        }
        final Location target = this.arena.getArenaSpawn().getBlock().getRelative(0, -1, 0).getLocation();
        target.getBlock().setBlockData(Bukkit.createBlockData(this.arena.getSpawnBlockData()), false);
    }

    private void restoreTeamSpawnBlocks() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (team.getSpawn() == null || team.getSpawnBlockData() == null) {
                continue;
            }
            final Location target = team.getSpawn().getBlock().getRelative(0, -1, 0).getLocation();
            target.getBlock().setBlockData(Bukkit.createBlockData(team.getSpawnBlockData()), false);
        }
    }

    public int getForgeLevel(final ArenaGenerator forge) {
        return this.forgeLevels.getOrDefault(forge, 0);
    }

    public @Nullable ForgeLevel getForgeUpgradeLevel(final ArenaGenerator forge) {
        final int next = this.getForgeLevel(forge) + 1;
        final List<ForgeLevel> levels = this.arena.getForgeLevels();
        if (levels == null) {
            return null;
        }
        for (final ForgeLevel fl : levels) {
            if (fl.level() == next) {
                return fl;
            }
        }
        return null;
    }

    public boolean upgradeForge(final ArenaGenerator forge) {
        final Integer level = this.forgeLevels.get(forge);
        if (level == null || level >= this.getForgeMaxLevel()) {
            return false;
        }
        this.forgeLevels.put(forge, level + 1);
        Bukkit.getPluginManager().callEvent(new GeneratorUpgradeEvent(this, forge, level, level + 1));
        this.rescheduleForge(forge);
        return true;
    }

    private void initGeneratorTicks() {
        this.generatorTicks.clear();
        for (final ArenaGenerator generator : this.arena.getGenerators()) {
            if (generator.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (generator.getLocation() == null) {
                continue;
            }
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.arena.getGeneratorConfigs();
            if (genConfigs == null) {
                continue;
            }
            final GeneratorConfig config = genConfigs.get(type);
            if (config == null) {
                continue;
            }
            final Material material = config.material();
            final long interval = config.interval();
            if (material == null || interval <= 0L) {
                continue;
            }
            this.generatorTicks.put(generator, new long[]{0L, interval, 0L});
        }
    }

    private void initForgeTicks() {
        this.forgeTicks.clear();
        this.forgeLevels.clear();
        for (final ArenaGenerator forge : this.arena.getGenerators()) {
            if (!forge.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (forge.getLocation() == null) {
                this.gameManager.getPlugin().getLogger().warning(this.lang.raw("log.game.forge_skipped", forge.getUniqueId()));
                continue;
            }
            this.forgeLevels.put(forge, Math.max(1, this.arena.getForgeDefaultLevel()));
            this.putForgeTicks(forge, this.forgeLevels.get(forge));
        }

    }

    private void putForgeTicks(final ArenaGenerator forge, final int level) {
        Map<Material, Long> intervals = null;
        var forgeLevels = this.arena.getForgeLevels();
        if (forgeLevels != null) {
            for (var fl : forgeLevels) {
                if (fl.level() == level) {
                    intervals = fl.intervals();
                    break;
                }
            }
        }
        if (intervals == null || intervals.isEmpty()) {
            this.gameManager.getPlugin().getLogger().warning(this.lang.raw("log.game.put_forge_ticks_warning", forge.getUniqueId(), level, forgeLevels == null ? "null" : forgeLevels.size()));
            return;
        }
        for (final var entry : intervals.entrySet()) {
            final Material material = entry.getKey();
            final long interval = entry.getValue();
            final String key = forgeKey(forge) + ":" + material.name();
            this.forgeTicks.put(key, new long[]{0L, interval, 0L});
        }
    }

    private void rescheduleForge(final ArenaGenerator forge) {
        final int level = this.forgeLevels.getOrDefault(forge, 1);
        final String prefix = forgeKey(forge) + ":";
        this.forgeTicks.keySet().removeIf(k -> k.startsWith(prefix));
        this.putForgeTicks(forge, level);
    }

    private int getForgeMaxLevel() {
        return Math.max(1, this.arena.getForgeMaxLevel());
    }

    private void stopGameTick() {
        if (this.gameTickTask != null) {
            this.gameTickTask.cancel();
            this.gameTickTask = null;
        }
    }

    private void startGameTick() {
        if (this.gameTickTask != null) {
            return;
        }
        this.gameTickTask = Bukkit.getScheduler().runTaskTimer(this.gameManager.getPlugin(), this::gameTick, 1L, 1L);
    }

    private void gameTick() {
        this.tick++;
        switch (this.state) {
            case STARTING:
                this.handleCountdownTick();
                break;
            case PLAYING:
                this.handleGeneratorTicks();
                this.handleForgeTicks();
                this.handleRespawnTicks();
                break;
            default:
                break;
        }
    }

    private void handleCountdownTick() {
        if (this.tick % 20 != 0) {
            return;
        }
        if (this.countdownSeconds <= 0) {
            this.start();
            return;
        }
        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (this.players.containsKey(p.getUniqueId())) {
                p.showTitle(Title.title(
                        Component.text("§e" + this.countdownSeconds),
                        this.lang.text("game.countdown_preparing"),
                        Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
            }
        }
        this.countdownSeconds--;
    }

    private void handleGeneratorTicks() {
        for (final Map.Entry<ArenaGenerator, long[]> entry : this.generatorTicks.entrySet()) {
            final ArenaGenerator generator = entry.getKey();
            if (generator.getLocation() == null) {
                continue;
            }
            final long[] data = entry.getValue();
            final long lastSpawn = data[0];
            final long interval = data[1];
            if (this.tick - lastSpawn < interval) {
                continue;
            }
            data[0] = this.tick;
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.arena.getGeneratorConfigs();
            final GeneratorConfig config = genConfigs != null ? genConfigs.get(type) : null;
            final Material material = config != null ? config.material() : this.gameManager.getConfigManager().getGeneratorMaterial(type);
            if (material == null) {
                continue;
            }
            final Location dropLocation = generator.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(entity -> entity instanceof org.bukkit.entity.Item)
                    .filter(entity -> ((org.bukkit.entity.Item) entity).getItemStack().getType() == material)
                    .count();
            if (nearbyCount >= 32) {
                continue;
            }
            final ItemStack stack = new ItemStack(material);
            final GeneratorSpawnEvent spawnEvent = new GeneratorSpawnEvent(generator, stack);
            Bukkit.getPluginManager().callEvent(spawnEvent);
            if (spawnEvent.isCancelled()) {
                continue;
            }
            dropLocation.getWorld().dropItem(dropLocation, spawnEvent.getItem(), item -> {
                item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                item.setPickupDelay(0);
            });
        }
    }

    private void handleForgeTicks() {
        for (final Map.Entry<String, long[]> entry : this.forgeTicks.entrySet()) {
            final long[] data = entry.getValue();
            final long lastSpawn = data[0];
            final long interval = data[1];
            if (this.tick - lastSpawn < interval) {
                continue;
            }
            data[0] = this.tick;
            final String key = entry.getKey();
            final int colon = key.indexOf(':');
            if (colon == -1) {
                continue;
            }
            final String locKey = key.substring(0, colon);
            final String matName = key.substring(colon + 1);
            final Material material = Material.matchMaterial(matName);
            if (material == null) {
                continue;
            }
            final ArenaGenerator forge = findForgeByKey(locKey);
            if (forge == null || forge.getLocation() == null) {
                continue;
            }
            final Location dropLocation = forge.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final World world = dropLocation.getWorld();
            if (world == null) {
                continue;
            }
            final long nearbyCount = world.getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(e -> e instanceof org.bukkit.entity.Item)
                    .filter(e -> ((org.bukkit.entity.Item) e).getItemStack().getType() == material)
                    .count();
            if (nearbyCount >= 32) {
                continue;
            }
            final ItemStack stack = new ItemStack(material);
            final GeneratorSpawnEvent spawnEvent = new GeneratorSpawnEvent(forge, stack);
            Bukkit.getPluginManager().callEvent(spawnEvent);
            if (spawnEvent.isCancelled()) {
                continue;
            }
            world.dropItem(dropLocation, spawnEvent.getItem(), item -> {
                item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                item.setPickupDelay(0);
            });
        }
    }

    private void handleRespawnTicks() {
        if (this.respawnTicks.isEmpty()) {
            return;
        }
        final var iter = this.respawnTicks.entrySet().iterator();
        while (iter.hasNext()) {
            final var entry = iter.next();
            final int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iter.remove();
                final Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    final ArenaTeam team = this.getPlayerTeam(player);
                    if (team != null) {
                        this.tryRespawn(player, team);
                    }
                }
            } else {
                entry.setValue(remaining);
                if (remaining % 20 == 0) {
                    final Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        final int seconds = remaining / 20;
                        player.showTitle(Title.title(
                                this.lang.text("game.died_title"),
                                this.lang.text("game.respawn_subtitle", seconds),
                                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
                    }
                }
            }
        }
    }

    private static String forgeKey(final ArenaGenerator forge) {
        final Location loc = forge.getLocation();
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private @Nullable ArenaGenerator findForgeByKey(final String locKey) {
        for (final ArenaGenerator gen : this.arena.getGenerators()) {
            if (!gen.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (gen.getLocation() == null) {
                continue;
            }
            if (forgeKey(gen).equals(locKey)) {
                return gen;
            }
        }
        return null;
    }

    private void startCountdown() {
        this.state = GameState.STARTING;
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, GameState.WAITING, GameState.STARTING));
        this.countdownSeconds = this.arena.getCountdown();
        this.startGameTick();
        this.debug("debug.countdown_started", this.arena.getName(),
                this.countdownSeconds, this.players.size());
    }

    private boolean hasAtLeastTwoTeams() {
        int filledTeams = 0;
        for (final var entry : this.teams.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                filledTeams++;
            }
        }
        return filledTeams >= 2;
    }

    private void updateCountdownState() {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            return;
        }
        if (this.players.size() < this.arena.getMinPlayers()) {
            this.cancelCountdown();
            return;
        }
        if (!this.hasAtLeastTwoTeams()) {
            if (this.state == GameState.STARTING) {
                this.cancelCountdown();
                final Component msg = this.lang.text(NamedTextColor.RED, "game.countdown_need_teams");
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (this.players.containsKey(p.getUniqueId())) {
                        p.sendMessage(msg);
                    }
                });
            }
            return;
        }
        if (this.state == GameState.WAITING && this.gameTickTask == null) {
            this.startCountdown();
        }
    }

    private void cancelCountdown() {
        if (this.state != GameState.STARTING) {
            return;
        }
        this.state = GameState.WAITING;
        this.stopGameTick();
        this.debug("debug.countdown_cancelled", this.arena.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, GameState.STARTING, GameState.WAITING));
        final Component langMsg = this.lang.text(NamedTextColor.RED, "game.countdown_cancelled");
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (this.players.containsKey(p.getUniqueId())) {
                p.sendMessage(langMsg);
                p.clearTitle();
            }
        });
    }

    public void killPlayer(final Player player) {
        final GamePlayer gp = this.players.get(player.getUniqueId());
        if (gp == null || !gp.isAlive()) {
            return;
        }
        final int oldDeaths = gp.getDeaths();
        gp.setAlive(false);
        gp.addDeath();

        final ArenaTeam team = gp.getTeam();
        Bukkit.getPluginManager().callEvent(new PlayerKillEvent(this, null, player, null, team));
        Bukkit.getPluginManager().callEvent(new GamePlayerDeathEvent(this, gp, DeathCause.CUSTOM));
        Bukkit.getPluginManager().callEvent(new GamePlayerStatChangeEvent(this, gp, StatType.DEATHS, oldDeaths, gp.getDeaths()));

        if (this.bedlessTeams.contains(team)) {
            Bukkit.getPluginManager().callEvent(new GamePlayerEliminateEvent(this, gp, null));
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            this.debug("debug.player_eliminated", player.getName(), this.arena.getName(),
                    team.getName());
            if (this.getAliveCount(team) == 0) {
                this.eliminateTeam(team);
            }
            return;
        }

        this.respawnTicks.put(player.getUniqueId(), this.arena.getRespawnDelay() * 20);
        player.showTitle(Title.title(
                this.lang.text("game.died_title"),
                this.lang.text("game.respawn_subtitle", this.arena.getRespawnDelay()),
                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
        this.debug("debug.player_died", player.getName(), this.arena.getName(),
                this.arena.getRespawnDelay());
        Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> player.spigot().respawn());
        this.startGameTick();
    }

    private void tryRespawn(final Player player, final ArenaTeam team) {
        this.respawnTicks.remove(player.getUniqueId());
        if (this.state != GameState.PLAYING) {
            return;
        }
        if (this.players.get(player.getUniqueId()) == null) {
            return;
        }
        if (this.bedlessTeams.contains(team)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            return;
        }
        if (team.getSpawn() == null) {
            return;
        }

        LocationUtil.safeTeleport(player, team.getSpawn());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        applyTeamArmor(player, team);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "game.respawned"));

        final GamePlayer gp = this.players.get(player.getUniqueId());
        if (gp != null) {
            gp.setAlive(true);
        }

        Bukkit.getPluginManager().callEvent(new PlayerRespawnEvent(this, player, team));
    }

    public void breakBed(final ArenaTeam team) {
        if (this.bedlessTeams.contains(team)) {
            return;
        }
        this.bedlessTeams.add(team);

        final Component msg = this.lang.text(NamedTextColor.RED, "game.bed_broken", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text(this.lang.raw("game.bed_broken_title")),
                Component.text(this.lang.raw("game.bed_broken_subtitle", team.getName().toUpperCase())),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(msg);
            final ArenaTeam pt = this.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(team.getName())) {
                p.showTitle(title);
            }
        });

        Bukkit.getPluginManager().callEvent(new BedBreakEvent(this, team, null));

        for (final UUID uuid : this.teams.get(team)) {
            final Integer rem = this.respawnTicks.remove(uuid);
            if (rem != null) {
                final GamePlayer gp = this.players.get(uuid);
                if (gp != null) {
                    Bukkit.getPluginManager().callEvent(new GamePlayerEliminateEvent(this, gp, null));
                }
            }
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && this.players.containsKey(uuid) && !this.players.get(uuid).isAlive()) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            }
        }

        if (this.getAliveCount(team) == 0) {
            this.eliminateTeam(team);
        }
    }

    private void eliminateTeam(final ArenaTeam team) {
        if (this.eliminatedTeams.contains(team)) {
            return;
        }
        this.eliminatedTeams.add(team);

        final Component msg = this.lang.text(NamedTextColor.GRAY, "game.team_eliminated", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text(this.lang.raw("game.team_eliminated_title")),
                Component.text(this.lang.raw("game.team_eliminated_subtitle", team.getName().toUpperCase())),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(msg);
            final ArenaTeam pt = this.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(team.getName())) {
                p.showTitle(title);
            }
        });

        Bukkit.getPluginManager().callEvent(new TeamEliminateEvent(this, team));

        for (final UUID uuid : this.teams.get(team)) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SPECTATOR);
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.you_eliminated"));
            }
        }

        this.checkWinCondition();
    }

    private void checkWinCondition() {
        if (this.state != GameState.PLAYING) {
            return;
        }
        ArenaTeam winner = null;
        int aliveTeams = 0;
        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.eliminatedTeams.contains(team)) {
                continue;
            }
            if (this.getAliveCount(team) > 0) {
                aliveTeams++;
                if (winner == null) {
                    winner = team;
                } else {
                    return;
                }
            }
        }

        if (winner != null) {
            this.endGame(winner);
        } else if (aliveTeams == 0 && this.state == GameState.PLAYING) {
            this.forceEnd();
        }
    }

    private void endGame(final ArenaTeam winner) {
        final GameState prevState = this.state;
        this.state = GameState.ENDING;
        this.stopGameTick();
        this.respawnTicks.clear();
        this.generatorTicks.clear();
        this.forgeTicks.clear();
        this.forgeLevels.clear();
        this.debug("debug.game_ended", this.arena.getName(), winner.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, prevState, GameState.ENDING));

        final Component msg = this.lang.text(NamedTextColor.GOLD, "game.team_wins", winner.getName().toUpperCase());
        final Title winTitle = Title.title(
                Component.text(this.lang.raw("game.win_title")),
                Component.text(this.lang.raw("game.win_subtitle", winner.getName().toUpperCase())),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(2))
        );
        final Title loseTitle = Title.title(
                Component.text(this.lang.raw("game.lose_title")),
                Component.text(this.lang.raw("game.lose_subtitle", winner.getName().toUpperCase())),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(2))
        );
        Bukkit.getOnlinePlayers().forEach(p -> {
            p.sendMessage(msg);
            final ArenaTeam pt = this.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(winner.getName())) {
                p.showTitle(winTitle);
            } else if (pt != null) {
                p.showTitle(loseTitle);
            }
        });

        Bukkit.getPluginManager().callEvent(new GameEndEvent(this, winner));

        // Coloca todos os jogadores em modo ADVENTURE no spawn para a celebracao final com suas armaduras
        for (final UUID uuid : this.players.keySet()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                continue;
            }
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            final ArenaTeam team = this.getPlayerTeam(p);
            if (team != null) {
                applyTeamArmor(p, team);
            }
            final Location spawn = this.arena.getArenaSpawn();
            if (spawn != null) {
                LocationUtil.safeTeleport(p, spawn);
            }
        }

        Bukkit.getScheduler().runTaskLater(
                this.gameManager.getPlugin(),
                () -> {
                    final Location lobby = this.gameManager.getConfigManager().getLobby();
                    final Location fallback = !Bukkit.getWorlds().isEmpty()
                            ? Bukkit.getWorlds().getFirst().getSpawnLocation() : null;
                    for (final UUID uuid : this.spectators) {
                        final Player player = Bukkit.getPlayer(uuid);
                        if (player == null) {
                            continue;
                        }
                        restoreInventory(player);
                        if (lobby != null && lobby.getWorld() != null) {
                            player.teleport(lobby);
                        } else if (fallback != null) {
                            player.teleport(fallback);
                        }
                        player.setGameMode(GameMode.SURVIVAL);
                        player.clearTitle();
                        this.gameManager.removePlayerMapping(player);
                    }
                    this.spectators.clear();
                    for (final var entry : this.teams.entrySet()) {
                        for (final UUID uuid : entry.getValue()) {
                            final Player player = Bukkit.getPlayer(uuid);
                            if (player == null) {
                                continue;
                            }
                            restoreInventory(player);
                            if (lobby != null && lobby.getWorld() != null) {
                                player.teleport(lobby);
                            } else if (fallback != null) {
                                player.teleport(fallback);
                            }
                            player.setGameMode(GameMode.SURVIVAL);
                            player.clearTitle();
                            this.gameManager.removePlayerMapping(player);
                        }
                    }
                    this.shopNpcManager.removeGameNpcs(this.arena.getWorldName());
                    this.players.clear();
                    this.teams.values().forEach(list -> list.clear());
                    this.eliminatedTeams.clear();
                    this.bedlessTeams.clear();
                    if (this.gameManager.getGameByWorld(this.arena.getWorldName()) == this) {
                        this.gameManager.removeGame(this.arena.getWorldName());
                    }
                },
                200L
        );
    }

    private @Nullable ArenaTeam findSmallestTeam() {
        return this.teams.keySet().stream()
                .filter(t -> !this.eliminatedTeams.contains(t))
                .min(Comparator.comparingInt(t -> this.teams.get(t).size()))
                .orElse(null);
    }

    private @Nullable ArenaTeam findNamedTeam(final String name) {
        for (final ArenaTeam team : this.teams.keySet()) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
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

    private int maxTeamSlots() {
        final ArenaMode mode = this.mode;
        if (mode != null) {
            return mode.getTeamSize();
        }
        final int teamCount = this.teams.size();
        if (teamCount == 0) {
            return 0;
        }
        return (int) Math.ceil((double) this.arena.getMinPlayers() / teamCount) + 1;
    }

    private int getAliveCount(final ArenaTeam team) {
        int count = 0;
        for (final UUID uuid : this.teams.get(team)) {
            final GamePlayer gp = this.players.get(uuid);
            if (gp != null && gp.isAlive()) {
                count++;
            }
        }
        return count;
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public boolean isFull() {
        final int capacity = this.arena.getTeams().size() * this.maxTeamSlots();
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

    public void broadcast(String message) {
        Component component = MiniMessage.miniMessage().deserialize(message);
        this.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> p.sendMessage(component));
    }


    public void forceEnd() {
        if (this.state == GameState.ENDING) {
            this.cleanupPlayersAndClose();
            return;
        }
        this.stopGameTick();
        this.respawnTicks.clear();
        this.generatorTicks.clear();
        this.forgeTicks.clear();
        this.forgeLevels.clear();
        final GameState prev = this.state;
        this.state = GameState.ENDING;
        this.debug("debug.game_force_ended", this.arena.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, prev, GameState.ENDING));
        this.cleanupPlayersAndClose();
    }

    private void cleanupPlayersAndClose() {
        final Location lobby = this.gameManager.getConfigManager().getLobby();
        for (final var entry : this.teams.entrySet()) {
            for (final UUID uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                restoreInventory(player);
                if (lobby != null && lobby.getWorld() != null) {
                    player.teleport(lobby);
                } else if (!Bukkit.getWorlds().isEmpty()) {
                    player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                }
                player.setGameMode(GameMode.SURVIVAL);
                player.clearTitle();
                this.gameManager.removePlayerMapping(player);
            }
        }
        for (final UUID uuid : this.spectators) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            restoreInventory(player);
            if (lobby != null && lobby.getWorld() != null) {
                player.teleport(lobby);
            } else if (!Bukkit.getWorlds().isEmpty()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            this.gameManager.removePlayerMapping(player);
        }
        this.shopNpcManager.removeGameNpcs(this.arena.getWorldName());
        this.spectators.clear();
        this.players.clear();
        this.teams.values().forEach(list -> list.clear());
        this.eliminatedTeams.clear();
        this.bedlessTeams.clear();
        this.placedBlocks.clear();
        this.gameManager.removeGame(this.arena.getWorldName());
    }

    private @Nullable ArenaTeam determineWinner() {
        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.eliminatedTeams.contains(team)) {
                continue;
            }
            if (this.getAliveCount(team) > 0) {
                return team;
            }
        }
        return null;
    }

    private ItemStack createExitDoorItem() {
        final ItemStack item = new ItemStack(Material.IRON_DOOR);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.exit_door.name")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.exit_door.lore"))
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static Material getWoolColor(final String dyeColor) {
        if (dyeColor == null) {
            return Material.WHITE_WOOL;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_WOOL;
            case "BLUE", "AZUL" -> Material.BLUE_WOOL;
            case "GREEN", "VERDE" -> Material.GREEN_WOOL;
            case "YELLOW", "AMARELO" -> Material.YELLOW_WOOL;
            case "PURPLE", "ROXO" -> Material.PURPLE_WOOL;
            case "PINK", "ROSA" -> Material.PINK_WOOL;
            case "ORANGE", "LARANJA" -> Material.ORANGE_WOOL;
            case "CYAN", "CIANO" -> Material.CYAN_WOOL;
            case "LIME" -> Material.LIME_WOOL;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_WOOL;
            case "GRAY", "CINZA" -> Material.GRAY_WOOL;
            case "BLACK", "PRETO" -> Material.BLACK_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private ItemStack createTeamSelectorItem(final @Nullable ArenaTeam team) {
        final Material material = team != null ? getWoolColor(team.getColor()) : Material.WHITE_WOOL;
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.team_selector.name")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.team_selector.lore"))
        ));
        item.setItemMeta(meta);
        return item;
    }

    public static Color getArmorColor(final String dyeColor) {
        if (dyeColor == null) {
            return Color.WHITE;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Color.fromRGB(255, 85, 85);
            case "BLUE", "AZUL" -> Color.fromRGB(85, 85, 255);
            case "GREEN", "VERDE" -> Color.fromRGB(85, 255, 85);
            case "YELLOW", "AMARELO" -> Color.fromRGB(255, 255, 85);
            case "PURPLE", "ROXO" -> Color.fromRGB(170, 85, 255);
            case "PINK", "ROSA" -> Color.fromRGB(255, 170, 170);
            case "ORANGE", "LARANJA" -> Color.fromRGB(255, 170, 85);
            case "CYAN", "CIANO" -> Color.fromRGB(85, 255, 255);
            case "LIME" -> Color.fromRGB(85, 255, 85);
            case "LIGHT_BLUE", "AZUL_CLARO" -> Color.fromRGB(85, 170, 255);
            case "GRAY", "CINZA" -> Color.fromRGB(170, 170, 170);
            case "BLACK", "PRETO" -> Color.fromRGB(0, 0, 0);
            default -> Color.WHITE;
        };
    }

    private static ItemStack coloredLeather(final Material material, final Color color) {
        final ItemStack item = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyTeamArmor(final Player player, final ArenaTeam team) {
        if (team == null || team.getColor() == null) {
            return;
        }
        final Color color = getArmorColor(team.getColor());
        player.getInventory().setHelmet(coloredLeather(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, color));
    }

    private void saveInventory(final Player player) {
        final PlayerStateManager manager = this.gameManager.getPlayerStateManager();
        if (manager.hasSavedState(player)) {
            if (!this.players.containsKey(player.getUniqueId()) && !this.spectators.contains(player.getUniqueId())) {
                this.gameManager.getPlugin().getLogger().warning(this.lang.raw("debug.player_orphan_snapshot", player.getUniqueId().toString()));
                manager.restorePlayerState(player);
            }
        }
        manager.savePlayerState(player);
    }

    private void restoreInventory(final Player player) {
        this.gameManager.getPlayerStateManager().restorePlayerState(player);
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this == this.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.showPlayer(this.gameManager.getPlugin(), online);
            online.showPlayer(this.gameManager.getPlugin(), player);
        }
    }
}
