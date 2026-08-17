package dev.sebastianjnuwu.bedwars.game;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.events.GameEndEvent;
import dev.sebastianjnuwu.bedwars.api.events.GameStateChangeEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Responsável pelo encerramento da partida: fim por limite de tempo, vitória,
 * ranking final, limpeza de jogadores/espectadores e remoção dos NPCs da loja.
 */
public final class GameEnding {

    private final Game game;
    private final GameTimeLimit timeLimit;

    /**
     * Cria o gerenciador de encerramento para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameEnding(final Game game) {
        this.game = game;
        this.timeLimit = new GameTimeLimit(game);
    }

    /**
     * Gerencia o limite de tempo da partida: envia avisos em marcos específicos,
     * exibe a contagem final e força o fim quando o tempo esgota.
     */
    public void handleTimeLimit() {
        this.timeLimit.handle();
    }

    /**
     * Encerra a partida com o time vencedor: para o tick, limpa o estado,
     * anuncia a vitória, exibe o ranking e agenda a limpeza final.
     *
     * @param winner time vencedor (não nulo)
     */
    public void endGame(final ArenaTeam winner) {
        final GameState prevState = this.game.state;
        this.game.state = GameState.ENDING;
        this.game.ticker().stopGameTick();
        this.game.respawnTicks.clear();
        this.game.pendingFinalRespawns.clear();
        this.game.generatorTicks.clear();
        this.game.forgeTicks.clear();
        this.game.forgeLevels.clear();
        this.game.debug("debug.game_ended", this.game.arena.getName(), winner.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this.game, prevState, GameState.ENDING));

        final Component msg = this.game.lang.text(NamedTextColor.GOLD, "game.team_wins", winner.getName().toUpperCase());
        final Title winTitle = Title.title(
                Component.text(this.game.lang.raw("game.win_title")),
                Component.text(this.game.lang.raw("game.win_subtitle", winner.getName().toUpperCase())),
                Title.Times.times(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(2))
        );
        final Title loseTitle = Title.title(
                Component.text(this.game.lang.raw("game.lose_title")),
                Component.text(this.game.lang.raw("game.lose_subtitle", winner.getName().toUpperCase())),
                Title.Times.times(java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(5), java.time.Duration.ofSeconds(2))
        );
        this.game.chat.broadcastWithSound(msg, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
        this.sendEndRanking();
        for (final Player p : this.game.chat.getPresentPlayers()) {
            final ArenaTeam pt = this.game.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(winner.getName())) {
                CompatProvider.chat().showTitle(p, winTitle);
            } else if (pt != null) {
                CompatProvider.chat().showTitle(p, loseTitle);
            }
        }

        Bukkit.getPluginManager().callEvent(new GameEndEvent(this.game, winner));

        // Coloca todos os jogadores em modo ADVENTURE no spawn para a celebracao final com suas armaduras
        for (final UUID uuid : this.game.players.keySet()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p == null) {
                continue;
            }
            p.setGameMode(GameMode.ADVENTURE);
            p.getInventory().clear();
            final ArenaTeam team = this.game.getPlayerTeam(p);
            if (team != null) {
                GameItems.applyTeamArmor(p, team);
            }
            final Location spawn = this.game.arena.getArenaSpawn();
            if (spawn != null) {
                LocationUtil.safeTeleport(p, spawn);
            }
        }

        Bukkit.getScheduler().runTaskLater(
                this.game.gameManager.getPlugin(),
                () -> {
                    final Location lobby = this.game.gameManager.getConfigManager().getLobby();
                    final Location fallback = !Bukkit.getWorlds().isEmpty()
                            ? Bukkit.getWorlds().getFirst().getSpawnLocation() : null;
                    for (final UUID uuid : this.game.spectators) {
                        final Player player = Bukkit.getPlayer(uuid);
                        if (player == null) {
                            continue;
                        }
                        this.game.lifecycle().restoreInventory(player);
                        if (lobby != null && lobby.getWorld() != null) {
                            player.teleport(lobby);
                        } else if (fallback != null) {
                            player.teleport(fallback);
                        }
                        player.setGameMode(GameMode.SURVIVAL);
                        CompatProvider.chat().clearTitle(player);
                        this.game.gameManager.removePlayerMapping(player);
                    }
                    this.game.spectators.clear();
                    for (final var entry : this.game.teams.entrySet()) {
                        for (final UUID uuid : entry.getValue()) {
                            final Player player = Bukkit.getPlayer(uuid);
                            if (player == null) {
                                continue;
                            }
                            this.game.lifecycle().restoreInventory(player);
                            if (lobby != null && lobby.getWorld() != null) {
                                player.teleport(lobby);
                            } else if (fallback != null) {
                                player.teleport(fallback);
                            }
                            player.setGameMode(GameMode.SURVIVAL);
                            CompatProvider.chat().clearTitle(player);
                            this.game.gameManager.removePlayerMapping(player);
                        }
                    }
                    this.game.shopNpcManager.removeGameNpcs(this.game.arena.getWorldName());
                    this.game.players.clear();
                    this.game.teams.values().forEach(list -> list.clear());
                    this.game.eliminatedTeams.clear();
                    this.game.bedlessTeams.clear();
                    if (this.game.gameManager.getGameByWorld(this.game.arena.getWorldName()) == this.game) {
                        this.game.gameManager.removeGame(this.game.arena.getWorldName());
                    }
                },
                200L
        );
    }

    /**
     * Exibe no chat o ranking dos jogadores da partida (top 3 por kills).
     */
    public void sendEndRanking() {
        final List<GamePlayer> top = this.game.getGamePlayers().stream()
                .sorted(Comparator.comparingInt((GamePlayer gp) -> gp.getKills()).reversed())
                .limit(3)
                .toList();
        final Component header = Component.text(this.game.lang.raw("game.rank_header"));
        this.game.chat.broadcast(header);
        if (top.isEmpty()) {
            this.game.chat.broadcast(Component.text(this.game.lang.raw("game.rank_empty")));
            return;
        }
        int position = 1;
        for (final GamePlayer gp : top) {
            final Player player = Bukkit.getPlayer(gp.getUuid());
            final String name = player != null ? player.getName() : gp.getUuid().toString();
            final String teamName = gp.getTeam() != null ? gp.getTeam().getName().toUpperCase() : "-";
            final Component line = Component.text(this.game.lang.raw(
                    "game.rank_line",
                    String.valueOf(position),
                    name,
                    teamName,
                    String.valueOf(gp.getKills()),
                    String.valueOf(gp.getDeaths())
            ));
            this.game.chat.broadcast(line);
            position++;
        }
    }

    /**
     * Força o fim da partida: para o tick, limpa o estado e remove a instância.
     */
    public void forceEnd() {
        if (this.game.state == GameState.ENDING) {
            this.cleanupPlayersAndClose();
            return;
        }
        this.game.ticker().stopGameTick();
        this.game.respawnTicks.clear();
        this.game.pendingFinalRespawns.clear();
        this.game.generatorTicks.clear();
        this.game.forgeTicks.clear();
        this.game.forgeLevels.clear();
        final GameState prev = this.game.state;
        this.game.state = GameState.ENDING;
        this.game.debug("debug.game_force_ended", this.game.arena.getName());
        Bukkit.getPluginManager().callEvent(new GameStateChangeEvent(this.game, prev, GameState.ENDING));
        this.cleanupPlayersAndClose();
    }

    private void cleanupPlayersAndClose() {
        final Location lobby = this.game.gameManager.getConfigManager().getLobby();
        for (final var entry : this.game.teams.entrySet()) {
            for (final UUID uuid : entry.getValue()) {
                final Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }
                this.game.lifecycle().restoreInventory(player);
                if (lobby != null && lobby.getWorld() != null) {
                    player.teleport(lobby);
                } else if (!Bukkit.getWorlds().isEmpty()) {
                    player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
                }
                player.setGameMode(GameMode.SURVIVAL);
                CompatProvider.chat().clearTitle(player);
                this.game.gameManager.removePlayerMapping(player);
            }
        }
        for (final UUID uuid : this.game.spectators) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            this.game.lifecycle().restoreInventory(player);
            if (lobby != null && lobby.getWorld() != null) {
                player.teleport(lobby);
            } else if (!Bukkit.getWorlds().isEmpty()) {
                player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            player.setGameMode(GameMode.SURVIVAL);
            this.game.gameManager.removePlayerMapping(player);
        }
        this.game.shopNpcManager.removeGameNpcs(this.game.arena.getWorldName());
        this.game.spectators.clear();
        this.game.players.clear();
        this.game.teams.values().forEach(list -> list.clear());
        this.game.eliminatedTeams.clear();
        this.game.bedlessTeams.clear();
        this.game.placedBlocks.clear();
        this.game.gameManager.removeGame(this.game.arena.getWorldName());
    }

    /**
     * Determina o time vencedor com base nos jogadores vivos não eliminados.
     *
     * @return o vencedor ou {@code null}
     */
    public ArenaTeam determineWinner() {
        for (final var entry : this.game.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.game.eliminatedTeams.contains(team)) {
                continue;
            }
            if (this.game.combat().getAliveCount(team) > 0) {
                return team;
            }
        }
        return null;
    }
}