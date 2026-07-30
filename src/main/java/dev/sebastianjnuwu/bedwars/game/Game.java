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
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.DeathCause;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Representa uma única instância de partida de BedWars. Gerencia a máquina de estados,
 * entrada/saída de jogadores, mortes/renascimentos, quebra de berços, eliminação de equipes,
 * condição de vitória e contagem regressiva de início automático.
 */
public class Game implements dev.sebastianjnuwu.bedwars.api.model.Game {


    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int RESPAWN_DELAY = 40;

    private final GameManager gameManager;
    private final ShopNpcManager shopNpcManager;
    private final LangManager lang;
    private final Arena arena;
    private final Map<ArenaTeam, List<UUID>> teams;
    private final Map<UUID, GamePlayer> players;
    private final Set<ArenaTeam> eliminatedTeams;
    private final Set<ArenaTeam> bedlessTeams;
    private final Set<UUID> spectators;
    private final Map<UUID, ItemStack[]> savedInventories;
    private final Map<UUID, ItemStack[]> savedArmor;
    private final Map<ArenaGenerator, Integer> forgeLevels;
    private final Map<ArenaGenerator, long[]> generatorTicks;
    private final Map<String, long[]> forgeTicks;
    private final Map<UUID, Integer> respawnTicks;
    private GameState state;
    private BukkitTask gameTickTask;
    private int tick;
    private int countdownSeconds;

    /**
     * Constrói uma nova instância de partida para a arena informada.
     * <p>
     * Inicializa todos os mapas internos e define o estado inicial como
     * {@link GameState#WAITING}. Os times são populados a partir da configuração
     * da arena — cada time começa com a lista de jogadores vazia.
     * </p>
     *
     * @param gameManager gerenciador de partidas que controla esta instância (não nulo)
     * @param arena       configuração da arena onde a partida será realizada (não nula)
     */
    public Game(final GameManager gameManager, final Arena arena, final ShopNpcManager shopNpcManager) {
        this.gameManager = gameManager;
        this.shopNpcManager = shopNpcManager;
        this.lang = gameManager.getLang();
        this.arena = arena;
        this.teams = new HashMap<>();
        this.players = new HashMap<>();
        this.eliminatedTeams = new HashSet<>();
        this.bedlessTeams = new HashSet<>();
        this.spectators = new HashSet<>();
        this.forgeLevels = new HashMap<>();
        this.generatorTicks = new HashMap<>();
        this.forgeTicks = new HashMap<>();
        this.respawnTicks = new HashMap<>();
        this.savedInventories = new HashMap<>();
        this.savedArmor = new HashMap<>();
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
        this.spectators.add(player.getUniqueId());

        // Salva inventario antes de limpar
        this.savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        this.savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(8, createExitDoorItem());
        player.getInventory().setItem(0, createTeamSelectorItem());
        final Location spawn = this.arena.getArenaSpawn();
        if (spawn != null) {
            LocationUtil.safeTeleport(player, spawn);
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(this.lang.text(NamedTextColor.GRAY, "game.spectating"));

        // Esconde jogadores de outras partidas
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this == this.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.hidePlayer(this.gameManager.getPlugin(), online);
            online.hidePlayer(this.gameManager.getPlugin(), player);
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
        return gp != null && gp.isAlive();
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

        Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this, player));

        this.savedInventories.put(player.getUniqueId(), player.getInventory().getContents());
        this.savedArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

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
        player.getInventory().setItem(0, createTeamSelectorItem());

        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this == this.gameManager.getPlayerGame(online)) {
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

        if (this.state == GameState.WAITING && this.gameTickTask == null
            && count >= this.arena.getMinPlayers()) {
            this.startCountdown();
        }
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
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "game.switched_team", newTeam.getName()));
    }

    public void leave(final Player player) {
        if (this.state == GameState.ENDING) {
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

        if (this.state == GameState.STARTING && this.players.size() < this.arena.getMinPlayers()) {
            this.cancelCountdown();
        }

        this.checkWinCondition();
    }

    public void start() {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) {
            return;
        }
        final GameState prevState = this.state;
        this.state = GameState.PLAYING;
        this.tick = 0;
        this.initGeneratorTicks();
        this.initForgeTicks();
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
                this.arena.getName(),
                this.arena.getShopNpcLocations(),
                this.arena.getShopNpcSkin()
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
                continue;
            }
            this.forgeLevels.put(forge, 1);
            this.putForgeTicks(forge, 1);
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
            final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
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
            dropLocation.getWorld().dropItem(dropLocation, spawnEvent.getItem(), item -> {
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

    @SuppressWarnings("deprecation")
    private void startCountdown() {
        this.state = GameState.STARTING;
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, GameState.WAITING, GameState.STARTING));
        this.countdownSeconds = this.arena.getCountdown();
        this.startGameTick();
    }

    private void cancelCountdown() {
        if (this.state != GameState.STARTING) {
            return;
        }
        this.state = GameState.WAITING;
        this.stopGameTick();
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
            if (this.getAliveCount(team) == 0) {
                this.eliminateTeam(team);
            }
            return;
        }

        this.respawnTicks.put(player.getUniqueId(), RESPAWN_DELAY);
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

        player.spigot().respawn();
        LocationUtil.safeTeleport(player, team.getSpawn());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
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
        ArenaTeam winner = null;
        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.eliminatedTeams.contains(team)) {
                continue;
            }
            if (this.getAliveCount(team) > 0) {
                if (winner == null) {
                    winner = team;
                } else {
                    return;
                }
            }
        }

        if (winner != null) {
            this.endGame(winner);
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
                            player.getInventory().clear();
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
                    this.shopNpcManager.removeGameNpcs(this.arena.getName());
                    this.players.clear();
                    this.teams.values().forEach(List::clear);
                    this.eliminatedTeams.clear();
                    this.bedlessTeams.clear();
                    if (this.gameManager.getGame(this.arena.getName()) == this) {
                        this.gameManager.removeGame(this.arena.getName());
                        this.gameManager.getArenaManager().resetArenaMap(this.arena.getName());
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

    private int maxTeamSlots() {
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
            return;
        }
        this.stopGameTick();
        this.respawnTicks.clear();
        this.generatorTicks.clear();
        this.forgeTicks.clear();
        this.forgeLevels.clear();
        final GameState prev = this.state;
        this.state = GameState.ENDING;
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this, prev, GameState.ENDING));
        for (final var entry : this.teams.entrySet()) {
            for (final UUID uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                restoreInventory(player);
                final Location lobby = this.gameManager.getConfigManager().getLobby();
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
        final Location lobby = this.gameManager.getConfigManager().getLobby();
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
        this.shopNpcManager.removeGameNpcs(this.arena.getName());
        this.spectators.clear();
        this.players.clear();
        this.teams.values().forEach(List::clear);
        this.eliminatedTeams.clear();
        this.bedlessTeams.clear();
        this.gameManager.removeGame(this.arena.getName());
        this.gameManager.getArenaManager().resetArenaMap(this.arena.getName());
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

    private ItemStack createTeamSelectorItem() {
        final ItemStack item = new ItemStack(Material.COMPASS);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.team_selector.name")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.team_selector.lore"))
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Restaura o inventario salvo do mundo normal do jogador.
     */
    private void restoreInventory(final Player player) {
        final UUID uuid = player.getUniqueId();
        final ItemStack[] contents = this.savedInventories.remove(uuid);
        final ItemStack[] armor = this.savedArmor.remove(uuid);
        player.getInventory().clear();
        if (contents != null) {
            player.getInventory().setContents(contents);
        }
        if (armor != null) {
            player.getInventory().setArmorContents(armor);
        }
        // Mostra jogadores de outras partidas novamente
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this == this.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.showPlayer(this.gameManager.getPlugin(), online);
            online.showPlayer(this.gameManager.getPlugin(), player);
        }
    }
}
