package dev.sebastianjnuwu.bedwars.game;

import dev.sebastianjnuwu.bedwars.api.events.BedBreakEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameEndEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameStartEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerJoinGameEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerKillEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerLeaveGameEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerRespawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.TeamEliminateEvent;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;

/**
 * Represents a single BedWars game instance. Manages the state machine,
 * player joins/leaves, deaths/respawns, bed breaks, team elimination,
 * win condition, and auto-start countdown.
 */
public class Game implements dev.sebastianjnuwu.bedwars.api.model.Game {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final int RESPAWN_DELAY = 40;

    private final GameManager gameManager;
    private final LangManager lang;
    private final Arena arena;
    private final Map<ArenaTeam, List<UUID>> teams;
    private final Map<UUID, GamePlayer> players;
    private final Set<ArenaTeam> eliminatedTeams;
    private final Set<ArenaTeam> bedlessTeams;
    private final Set<UUID> spectators;
    private final Map<UUID, BukkitTask> respawnTasks;
    private final Map<ArenaGenerator, Integer> forgeLevels;
    private final Map<ArenaGenerator, List<BukkitTask>> forgeTasks;
    private final Map<ArenaGenerator, BukkitTask> generatorTasks;
    private GameState state;
    private BukkitTask countdownTask;
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
    public Game(final GameManager gameManager, final Arena arena) {
        this.gameManager = gameManager;
        this.lang = gameManager.getLang();
        this.arena = arena;
        this.teams = new HashMap<>();
        this.players = new HashMap<>();
        this.eliminatedTeams = new HashSet<>();
        this.bedlessTeams = new HashSet<>();
        this.spectators = new HashSet<>();
        this.respawnTasks = new HashMap<>();
        this.forgeLevels = new HashMap<>();
        this.forgeTasks = new HashMap<>();
        this.generatorTasks = new HashMap<>();
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
        player.getInventory().clear();
        player.getInventory().setItem(8, createExitDoorItem());
        final Location spawn = this.arena.getArenaSpawn();
        if (spawn != null) {
            LocationUtil.safeTeleport(player, spawn);
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.sendMessage(this.lang.text(NamedTextColor.GRAY, "game.spectating"));
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
        this.join(player, null);
    }

    public void join(final Player player, final @Nullable String teamName) {
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

        final Location spawn = this.arena.getArenaSpawn();
        if (spawn != null) {
            LocationUtil.safeTeleport(player, spawn);
        }
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().setItem(8, createExitDoorItem());

        final int count = this.players.size();
        final int max = this.arena.getTeams().size();
        final Component msg = this.lang.text(NamedTextColor.GREEN, "game.join_broadcast",
                player.getName(), String.valueOf(count), String.valueOf(max));
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));

        if (this.state == GameState.WAITING && this.countdownTask == null
                && count >= this.arena.getMinPlayers()) {
            this.startCountdown();
        }
    }

    public void leave(final Player player) {
        if (this.state == GameState.ENDING) return;

        final boolean wasSpectator = this.spectators.remove(player.getUniqueId());
        if (wasSpectator) {
            final Location lobby = this.gameManager.getConfigManager().getLobby();
            if (lobby != null) {
                player.teleport(lobby);
            } else if (!Bukkit.getWorlds().isEmpty()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            return;
        }

        final BukkitTask task = this.respawnTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        final GamePlayer gp = this.players.remove(player.getUniqueId());
        if (gp == null) return;

        final ArenaTeam team = gp.getTeam();
        this.teams.get(team).remove(player.getUniqueId());

        player.getInventory().clear();
        final Location lobby = this.gameManager.getConfigManager().getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
        player.setGameMode(GameMode.SURVIVAL);

        final Component msg = this.lang.text(NamedTextColor.YELLOW, "game.leave_broadcast", player.getName());
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));

        Bukkit.getPluginManager().callEvent(new PlayerLeaveGameEvent(this, player));

        if (this.countdownTask != null && this.players.size() < this.arena.getMinPlayers()) {
            this.cancelCountdown();
        }

        this.checkWinCondition();
    }

    private void stopCountdown() {
        if (this.countdownTask != null) {
            this.countdownTask.cancel();
            this.countdownTask = null;
        }
    }

    public void start() {
        if (this.state != GameState.WAITING && this.state != GameState.STARTING) return;
        this.stopCountdown();
        this.state = GameState.PLAYING;

        this.restoreArenaSpawnBlock();
        this.restoreTeamSpawnBlocks();
        this.startForges();
        this.startGlobalGenerators();

        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            final Location spawnLoc = team.getSpawn();
            if (spawnLoc == null) continue;
            int index = 0;
            for (final UUID uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) continue;
                final Location spawn = spawnLoc.clone().add(index * 0.5, 0, 0);
                LocationUtil.safeTeleport(player, spawn);
                player.getInventory().clear();
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setExp(0);
                player.setLevel(0);
                final GamePlayer gp = this.players.get(uuid);
                if (gp != null) gp.setAlive(true);
                index++;
            }
        }

        final Title startTitle = Title.title(
                Component.text("§6§lBEDWARS"),
                Component.text("§eProteja sua cama e elimine os times!"),
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
    }

    private void restoreArenaSpawnBlock() {
        if (this.arena.getArenaSpawn() == null || this.arena.getSpawnBlockData() == null) return;
        final Location target = this.arena.getArenaSpawn().getBlock().getRelative(0, -1, 0).getLocation();
        target.getBlock().setBlockData(Bukkit.createBlockData(this.arena.getSpawnBlockData()), false);
    }

    private void restoreTeamSpawnBlocks() {
        for (final ArenaTeam team : this.arena.getTeams()) {
            if (team.getSpawn() == null || team.getSpawnBlockData() == null) continue;
            final Location target = team.getSpawn().getBlock().getRelative(0, -1, 0).getLocation();
            target.getBlock().setBlockData(Bukkit.createBlockData(team.getSpawnBlockData()), false);
        }
    }

    /** Returns the current level of a forge in this match, or zero when it is not active. */
    public int getForgeLevel(final ArenaGenerator forge) {
        return this.forgeLevels.getOrDefault(forge, 0);
    }

    /**
     * Upgrades an active forge by one level. This is intentionally independent of a shop;
     * a future shop can charge the player and then invoke this method.
     *
     * @return true when the forge was upgraded, false when it is invalid or at the maximum level
     */
    public boolean upgradeForge(final ArenaGenerator forge) {
        final Integer level = this.forgeLevels.get(forge);
        if (level == null || level >= this.getForgeMaxLevel()) return false;
        this.forgeLevels.put(forge, level + 1);
        this.scheduleForge(forge);
        return true;
    }

    private void startGlobalGenerators() {
        for (final ArenaGenerator generator : this.arena.getGenerators()) {
            if (generator.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            this.scheduleGlobalGenerator(generator);
        }
    }

    private void scheduleGlobalGenerator(final ArenaGenerator generator) {
        final BukkitTask oldTask = this.generatorTasks.remove(generator);
        if (oldTask != null) {
            oldTask.cancel();
        }

        final String type = generator.getType().toLowerCase();
        final Material material = this.gameManager.getConfigManager().getGeneratorMaterial(type);
        final long interval = this.gameManager.getConfigManager().getGeneratorInterval(type);
        if (material == null || interval <= 0L || generator.getLocation() == null) {
            return;
        }

        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.gameManager.getPlugin(), () -> {
            if (this.state != GameState.PLAYING) {
                return;
            }
            final Location dropLocation = generator.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(entity -> entity instanceof org.bukkit.entity.Item)
                    .filter(entity -> ((org.bukkit.entity.Item) entity).getItemStack().getType() == material)
                    .count();
            if (nearbyCount >= 32) {
                return;
            }
            dropLocation.getWorld().dropItem(dropLocation, new ItemStack(material), item -> {
                item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                item.setPickupDelay(0);
            });
        }, interval, interval);
        this.generatorTasks.put(generator, task);
    }

    private void stopGlobalGenerators() {
        this.generatorTasks.values().forEach(BukkitTask::cancel);
        this.generatorTasks.clear();
    }

    private void startForges() {
        for (final ArenaGenerator generator : this.arena.getGenerators()) {
            if (!generator.getType().equalsIgnoreCase("forge")) continue;
            this.forgeLevels.put(generator, 1);
            this.scheduleForge(generator);
        }
    }

    private void scheduleForge(final ArenaGenerator forge) {
        final List<BukkitTask> oldTasks = this.forgeTasks.remove(forge);
        if (oldTasks != null) oldTasks.forEach(BukkitTask::cancel);

        final int level = this.forgeLevels.getOrDefault(forge, 1);
        final Map<Material, Long> intervals = this.gameManager.getConfigManager().getForgeIntervals(level);
        final List<BukkitTask> tasks = new ArrayList<>();
        for (final var entry : intervals.entrySet()) {
            final Material material = entry.getKey();
            final long interval = entry.getValue();
            tasks.add(Bukkit.getScheduler().runTaskTimer(this.gameManager.getPlugin(), () -> {
                if (this.state != GameState.PLAYING) return;
                final Location dropLocation = forge.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);

                // Cap: don't spawn if there are already 32+ of this material nearby
                final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
                        .filter(e -> e instanceof org.bukkit.entity.Item)
                        .filter(e -> ((org.bukkit.entity.Item) e).getItemStack().getType() == material)
                        .count();
                if (nearbyCount >= 32) return;

                dropLocation.getWorld().dropItem(dropLocation, new ItemStack(material), item -> {
                    item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                    item.setPickupDelay(0);
                });
            }, interval, interval));
        }
        this.forgeTasks.put(forge, tasks);
    }

    private int getForgeMaxLevel() {
        return this.gameManager.getConfigManager().getForgeMaxLevel();
    }

    private void stopForges() {
        this.forgeTasks.values().forEach(tasks -> tasks.forEach(BukkitTask::cancel));
        this.forgeTasks.clear();
        this.forgeLevels.clear();
        this.stopGlobalGenerators();
    }

    @SuppressWarnings("deprecation")
    private void startCountdown() {
        this.state = GameState.STARTING;
        this.countdownSeconds = this.arena.getCountdown();

        this.countdownTask = Bukkit.getScheduler().runTaskTimer(
                this.gameManager.getPlugin(),
                () -> {
                    if (this.countdownSeconds <= 0) {
                        this.start();
                        return;
                    }
                    if (this.state != GameState.STARTING) {
                        this.cancelCountdown();
                        return;
                    }
                    for (final Player p : Bukkit.getOnlinePlayers()) {
                        if (this.players.containsKey(p.getUniqueId())) {
                            p.sendTitle("§e" + this.countdownSeconds, "§7Preparando...", 0, 20, 10);
                        }
                    }
                    this.countdownSeconds--;
                },
                0L,
                20L
        );
    }

    private void cancelCountdown() {
        this.stopCountdown();
        if (this.state != GameState.STARTING) return;
        this.state = GameState.WAITING;
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
        if (gp == null || !gp.isAlive()) return;
        gp.setAlive(false);
        gp.addDeath();

        final ArenaTeam team = gp.getTeam();
        Bukkit.getPluginManager().callEvent(new PlayerKillEvent(this, null, player, null, team));

        if (this.bedlessTeams.contains(team)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            if (this.getAliveCount(team) == 0) {
                this.eliminateTeam(team);
            }
            return;
        }

        final BukkitTask task = Bukkit.getScheduler().runTaskLater(
                this.gameManager.getPlugin(),
                () -> this.tryRespawn(player, team),
                RESPAWN_DELAY
        );
        this.respawnTasks.put(player.getUniqueId(), task);
    }

    private void tryRespawn(final Player player, final ArenaTeam team) {
        this.respawnTasks.remove(player.getUniqueId());
        if (this.state != GameState.PLAYING) return;
        if (this.players.get(player.getUniqueId()) == null) return;
        if (this.bedlessTeams.contains(team)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            return;
        }
        if (team.getSpawn() == null) return;

        player.spigot().respawn();
        LocationUtil.safeTeleport(player, team.getSpawn());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "game.respawned"));

        final GamePlayer gp = this.players.get(player.getUniqueId());
        if (gp != null) gp.setAlive(true);

        Bukkit.getPluginManager().callEvent(new PlayerRespawnEvent(this, player, team));
    }

    public void breakBed(final ArenaTeam team) {
        if (this.bedlessTeams.contains(team)) return;
        this.bedlessTeams.add(team);

        final Component msg = this.lang.text(NamedTextColor.RED, "game.bed_broken", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text("§c§lCAMA DESTRUÍDA!"),
                Component.text("§eTime " + team.getName().toUpperCase() + " §7perdeu a cama!"),
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
            final BukkitTask task = this.respawnTasks.remove(uuid);
            if (task != null) {
                task.cancel();
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
        if (this.eliminatedTeams.contains(team)) return;
        this.eliminatedTeams.add(team);

        final Component msg = this.lang.text(NamedTextColor.GRAY, "game.team_eliminated", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text("§c§lTIME ELIMINADO!"),
                Component.text("§e" + team.getName().toUpperCase() + " §7foi eliminado!"),
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
            if (this.eliminatedTeams.contains(team)) continue;
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
        this.state = GameState.ENDING;
        this.stopForges();

        final Component msg = this.lang.text(NamedTextColor.GOLD, "game.team_wins", winner.getName().toUpperCase());
        final Title winTitle = Title.title(
                Component.text("§6§lVITÓRIA!"),
                Component.text("§eTime " + winner.getName().toUpperCase() + " §7venceu a partida!"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(5), Duration.ofSeconds(2))
        );
        final Title loseTitle = Title.title(
                Component.text("§c§lDERROTA!"),
                Component.text("§7O time " + winner.getName().toUpperCase() + " §7venceu!"),
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

        for (final BukkitTask task : this.respawnTasks.values()) {
            task.cancel();
        }
        this.respawnTasks.clear();

        Bukkit.getScheduler().runTaskLater(
                this.gameManager.getPlugin(),
                () -> {
                    final Location lobby = this.gameManager.getConfigManager().getLobby();
                    final Location fallback = !Bukkit.getWorlds().isEmpty()
                            ? Bukkit.getWorlds().getFirst().getSpawnLocation() : null;
                    for (final UUID uuid : this.spectators) {
                        final Player player = Bukkit.getPlayer(uuid);
                        if (player == null) continue;
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
                            if (player == null) continue;
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
        if (teamCount == 0) return 0;
        return (int) Math.ceil((double) this.arena.getMinPlayers() / teamCount) + 1;
    }

    private int getAliveCount(final ArenaTeam team) {
        int count = 0;
        for (final UUID uuid : this.teams.get(team)) {
            final GamePlayer gp = this.players.get(uuid);
            if (gp != null && gp.isAlive()) count++;
        }
        return count;
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public Collection<GamePlayer> getGamePlayers() {
        return this.players.values();
    }

    public Map<ArenaTeam, List<UUID>> getTeams() {
        return this.teams;
    }

    public void forceEnd() {
        if (this.state == GameState.ENDING) return;
        this.stopForges();
        final ArenaTeam winner = this.determineWinner();
        if (winner != null) {
            this.endGame(winner);
        } else {
            this.state = GameState.ENDING;
            for (final BukkitTask task : this.respawnTasks.values()) {
                task.cancel();
            }
            this.respawnTasks.clear();
            for (final var entry : this.teams.entrySet()) {
                for (final UUID uuid : entry.getValue()) {
                    final Player player = Bukkit.getPlayer(uuid);
                    if (player == null) continue;
                    player.getInventory().clear();
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
                if (player == null) continue;
                if (lobby != null && lobby.getWorld() != null) {
                    player.teleport(lobby);
                } else if (!Bukkit.getWorlds().isEmpty()) {
                    player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                }
                player.setGameMode(GameMode.SURVIVAL);
                this.gameManager.removePlayerMapping(player);
            }
            this.spectators.clear();
            this.players.clear();
            this.teams.values().forEach(List::clear);
            this.eliminatedTeams.clear();
            this.bedlessTeams.clear();
            this.gameManager.removeGame(this.arena.getName());
            this.gameManager.getArenaManager().resetArenaMap(this.arena.getName());
        }
    }

    private @Nullable ArenaTeam determineWinner() {
        for (final var entry : this.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.eliminatedTeams.contains(team)) continue;
            if (this.getAliveCount(team) > 0) {
                return team;
            }
        }
        return null;
    }

    private ItemStack createExitDoorItem() {
        final ItemStack item = new ItemStack(Material.IRON_DOOR);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.confirm_exit.no_cancel")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.confirm_exit.no_cancel_desc"))
        ));
        item.setItemMeta(meta);
        return item;
    }
}
