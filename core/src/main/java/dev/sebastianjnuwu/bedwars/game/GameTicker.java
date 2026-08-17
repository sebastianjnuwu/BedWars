package dev.sebastianjnuwu.bedwars.game;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.events.GameStartEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameStateChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorSpawnEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Responsável pelo loop de ticks da partida: contagem regressiva, geradores de
 * recursos, renascimentos e início do jogo. Também gerencia a task de tick e a
 * transição de estado para {@link GameState#PLAYING}.
 */
public final class GameTicker {

    private final Game game;

    /**
     * Cria o gerenciador de ticks para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameTicker(final Game game) {
        this.game = game;
    }

    /**
     * Inicia a partida se houver times suficientes.
     */
    public void start() {
        if (this.game.state != GameState.WAITING && this.game.state != GameState.STARTING) {
            return;
        }
        if (!this.hasEnoughActiveTeams()) {
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
        if (this.game.state != GameState.WAITING && this.game.state != GameState.STARTING) {
            return;
        }
        this.startPlaying();
    }

    private void startPlaying() {
        final GameState prevState = this.game.state;
        this.game.state = GameState.PLAYING;
        this.game.tick = 0;
        this.game.gameManager.getArenaManager().markWorldDirty(
                this.game.arena.getWorldName() != null ? this.game.arena.getWorldName() : "bw_" + this.game.arena.getName());
        this.initGeneratorTicks();
        this.game.upgrades().initForgeTicks();
        this.startGameTick();
        this.game.debug("debug.game_started", this.game.arena.getName(), this.game.players.size());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this.game, prevState, GameState.PLAYING));

        this.restoreArenaSpawnBlock();
        this.restoreTeamSpawnBlocks();

        for (final var entry : this.game.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            final Location spawnLoc = team.getSpawn();
            if (spawnLoc == null) {
                continue;
            }
            int index = 0;
            for (final var uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                final Location spawn = spawnLoc.clone().add(index * 0.5, 0, 0);
                LocationUtil.safeTeleport(player, spawn);
                player.getInventory().clear();
                player.getEnderChest().clear();
                GameItems.applyTeamArmor(player, team);
                this.game.items().giveSpawnItems(player);
                this.game.upgrades().applyTeamUpgrades(player, team);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.setExp(0);
                player.setLevel(0);
                final var gp = this.game.players.get(uuid);
                if (gp != null) {
                    gp.setAlive(true);
                }
                index++;
            }
        }

        final Title startTitle = Title.title(
                Component.text(this.game.lang.raw("game.start_title")),
                Component.text(this.game.lang.raw("game.start_subtitle")),
                Title.Times.times(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(4), java.time.Duration.ofSeconds(1))
        );
        final Component msg = this.game.lang.text(NamedTextColor.GOLD, "game.started");
        this.game.chat.showTitle(startTitle);
        this.game.chat.sendToPlayers(msg);

        Bukkit.getPluginManager().callEvent(new GameStartEvent(this.game));

        this.game.shopNpcManager.spawnGameNpcs(
                this.game.arena.getWorldName(),
                this.game.arena.getShopNpcs()
        );
    }

    private void restoreArenaSpawnBlock() {
        if (this.game.arena.getArenaSpawn() == null || this.game.arena.getSpawnBlockData() == null) {
            return;
        }
        final Location target = this.game.arena.getArenaSpawn().getBlock().getRelative(0, -1, 0).getLocation();
        target.getBlock().setBlockData(Bukkit.createBlockData(this.game.arena.getSpawnBlockData()), false);
    }

    private void restoreTeamSpawnBlocks() {
        for (final ArenaTeam team : this.game.arena.getTeams()) {
            if (team.getSpawn() == null || team.getSpawnBlockData() == null) {
                continue;
            }
            final Location target = team.getSpawn().getBlock().getRelative(0, -1, 0).getLocation();
            target.getBlock().setBlockData(Bukkit.createBlockData(team.getSpawnBlockData()), false);
        }
    }

    void stopGameTick() {
        if (this.game.gameTickTask != null) {
            this.game.gameTickTask.cancel();
            this.game.gameTickTask = null;
        }
    }

    void startGameTick() {
        if (this.game.gameTickTask != null) {
            return;
        }
        this.game.gameTickTask = Bukkit.getScheduler().runTaskTimer(this.game.gameManager.getPlugin(), this::gameTick, 1L, 1L);
    }

    private boolean isMatchWorldLoaded() {
        final String worldName = this.game.arena.getWorldName() != null ? this.game.arena.getWorldName() : "bw_" + this.game.arena.getName();
        return Bukkit.getWorld(worldName) != null;
    }

    private void gameTick() {
        if (this.game.state == GameState.STARTING || this.game.state == GameState.PLAYING) {
            if (!this.isMatchWorldLoaded()) {
                this.game.debug("debug.game_force_ended", this.game.arena.getName());
                this.game.ending().forceEnd();
                return;
            }
        }
        this.game.tick++;
        switch (this.game.state) {
            case STARTING:
                this.handleCountdownTick();
                break;
            case PLAYING:
                this.handleGeneratorTicks();
                this.game.upgrades().handleForgeTicks();
                this.handleRespawnTicks();
                this.game.ending().handleTimeLimit();
                break;
            default:
                break;
        }
    }

    private void handleCountdownTick() {
        if (this.game.tick % 20 != 0) {
            return;
        }
        if (this.game.countdownSeconds <= 0) {
            this.start();
            return;
        }
        final int beepStart = Math.max(1, (int) Math.ceil(this.game.arena.getCountdown() * 0.2));
        for (final Player p : Bukkit.getOnlinePlayers()) {
            if (this.game.players.containsKey(p.getUniqueId())) {
                CompatProvider.chat().showTitle(p, Title.title(
                        Component.text("§e" + this.game.countdownSeconds),
                        this.game.lang.text("game.countdown_preparing"),
                        Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
            }
        }
        if (this.game.countdownSeconds <= beepStart) {
            this.game.chat.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, this.countdownPitch(this.game.countdownSeconds, beepStart));
        }
        this.game.countdownSeconds--;
    }

    /**
     * Calcula o tom (pitch) do bip de contagem regressiva.
     *
     * @param remaining segundos restantes
     * @param beepStart limite em que os bipes começam
     * @return tom do som
     */
    static float countdownPitch(final int remaining, final int beepStart) {
        return remaining <= 10 ? 1.0F + (beepStart - remaining) * 0.05F : 0.8F;
    }

    private int currentGeneratorLevel() {
        final Map<Integer, Integer> levelTimes = this.game.arena.getLevelTimes();
        if (levelTimes == null || levelTimes.isEmpty()) {
            return 1;
        }
        final int minutes = (int) (this.game.tick / (20L * 60L));
        int level = 1;
        for (final var entry : levelTimes.entrySet()) {
            if (entry.getKey() <= minutes && entry.getValue() > level) {
                level = entry.getValue();
            }
        }
        return level;
    }

    private void initGeneratorTicks() {
        this.game.generatorTicks.clear();
        for (final ArenaGenerator generator : this.game.arena.getGenerators()) {
            if (generator.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (generator.getLocation() == null) {
                continue;
            }
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.game.arena.getGeneratorConfigs();
            if (genConfigs == null) {
                continue;
            }
            final GeneratorConfig config = genConfigs.get(type);
            if (config == null) {
                continue;
            }
            final Material material = config.material();
            final long interval = config.intervalForLevel(this.currentGeneratorLevel());
            if (material == null || interval <= 0L) {
                continue;
            }
            this.game.generatorTicks.put(generator, new long[]{0L, interval, 0L});
        }
    }

    private void handleGeneratorTicks() {
        for (final Map.Entry<ArenaGenerator, long[]> entry : this.game.generatorTicks.entrySet()) {
            final ArenaGenerator generator = entry.getKey();
            if (generator.getLocation() == null) {
                continue;
            }
            final long[] data = entry.getValue();
            final long lastSpawn = data[0];
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.game.arena.getGeneratorConfigs();
            final GeneratorConfig config = genConfigs != null ? genConfigs.get(type) : null;
            final Material material = config != null ? config.material() : this.game.gameManager.getConfigManager().getGeneratorMaterial(type);
            if (material == null) {
                continue;
            }
            final long interval = config != null
                    ? config.intervalForLevel(this.currentGeneratorLevel())
                    : this.game.gameManager.getConfigManager().getGeneratorInterval(type);
            if (interval <= 0L || this.game.tick - lastSpawn < interval) {
                continue;
            }
            data[0] = this.game.tick;
            data[1] = interval;
            final Location dropLocation = generator.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(entity -> entity instanceof Item)
                    .filter(entity -> ((Item) entity).getItemStack().getType() == material)
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

    private void handleRespawnTicks() {
        if (this.game.respawnTicks.isEmpty()) {
            return;
        }
        final var iter = this.game.respawnTicks.entrySet().iterator();
        while (iter.hasNext()) {
            final var entry = iter.next();
            final int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                iter.remove();
                final Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    final ArenaTeam team = this.game.getPlayerTeam(player);
                    if (team != null) {
                        this.game.combat().tryRespawn(player, team);
                    }
                }
            } else {
                entry.setValue(remaining);
                if (remaining % 20 == 0) {
                    final Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        final int seconds = remaining / 20;
                        CompatProvider.chat().showTitle(player, Title.title(
                                this.game.lang.text("game.died_title"),
                                this.game.lang.text("game.respawn_subtitle", seconds),
                                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
                    }
                }
            }
        }
    }

    /**
     * Inicia a contagem regressiva automática quando há times suficientes.
     */
    public void startCountdown() {
        this.game.state = GameState.STARTING;
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this.game, GameState.WAITING, GameState.STARTING));
        this.game.countdownSeconds = this.game.arena.getCountdown();
        this.startGameTick();
        this.game.debug("debug.countdown_started", this.game.arena.getName(),
                this.game.countdownSeconds, this.game.players.size());
    }

    /**
     * Verifica se há times suficientes para iniciar a partida.
     *
     * @return {@code true} se o mínimo de times ativos foi atingido
     */
    public boolean hasEnoughActiveTeams() {
        return this.countActiveTeams() >= this.game.arena.getMinTeamsToStart();
    }

    /**
     * Conta quantos times têm o mínimo de jogadores por time.
     *
     * @return número de times ativos
     */
    public long countActiveTeams() {
        final int minPerTeam = Math.max(1, this.game.arena.getMinPlayersPerTeam());
        return this.game.teams.values().stream()
                .filter(members -> members.size() >= minPerTeam)
                .count();
    }

    /**
     * Atualiza o estado da contagem regressiva conforme os jogadores entram/saem.
     */
    public void updateCountdownState() {
        if (this.game.state != GameState.WAITING && this.game.state != GameState.STARTING) {
            return;
        }
        if (!this.hasEnoughActiveTeams()) {
            this.cancelCountdown();
            return;
        }
        if (this.game.state == GameState.WAITING && this.game.gameTickTask == null) {
            this.startCountdown();
        }
    }

    /**
     * Cancela a contagem regressiva e volta para o estado de espera.
     */
    public void cancelCountdown() {
        if (this.game.state != GameState.STARTING) {
            return;
        }
        this.game.state = GameState.WAITING;
        this.stopGameTick();
        this.game.debug("debug.countdown_cancelled", this.game.arena.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this.game, GameState.STARTING, GameState.WAITING));
        final Component langMsg = this.game.lang.text(NamedTextColor.RED, "game.countdown_cancelled");
        this.game.chat.sendToPlayers(langMsg);
        this.game.chat.clearTitle();
    }
}