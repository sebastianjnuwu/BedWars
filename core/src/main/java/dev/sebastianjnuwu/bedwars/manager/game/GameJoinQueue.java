package dev.sebastianjnuwu.bedwars.manager.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Responsável pelas filas de entrada pendentes de partidas.
 * <p>
 * Enfileira jogadores cuja arena ainda não tem mundo construído, dispara a
 * construção assíncrona da instância e, quando pronta, teleporta todos os
 * jogadores da fila para dentro da partida.
 * </p>
 */
class GameJoinQueue {

    private final GameManager manager;
    private final Set<String> buildingArenas;
    private final Map<String, List<PendingJoin>> pendingJoins;

    GameJoinQueue(final GameManager manager) {
        this.manager = manager;
        this.buildingArenas = new HashSet<>();
        this.pendingJoins = new HashMap<>();
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
    void enqueueJoin(final Player player, final String arenaName, final @Nullable String teamName, final @Nullable ArenaMode mode, final boolean teleport) {
        final String key = queueKey(arenaName, mode);
        final List<PendingJoin> queue = this.pendingJoins.computeIfAbsent(key, k -> new ArrayList<>());
        final UUID playerId = player.getUniqueId();
        if (queue.stream().anyMatch(pending -> pending.playerId().equals(playerId))) {
            CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }
        queue.add(new PendingJoin(playerId, teamName, mode, teleport));
        if (this.buildingArenas.contains(key)) {
            CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.YELLOW, "game.countdown_preparing"));
            return;
        }
        this.buildingArenas.add(key);
        CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.YELLOW, "game.countdown_preparing"));
        this.manager.arenaManager.createInstanceAsync(arenaName, instance -> this.completePendingJoins(arenaName, mode, instance));
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
     * @param mode      modo de partida ou {@code null}
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
                        CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.RED, "game.world_not_ready", arenaName));
                    }
                }
            }
            return;
        }
        final List<String> missing = this.manager.validateArena(instance);
        if (!missing.isEmpty()) {
            this.manager.arenaManager.deleteInstanceWorld(instance.getWorldName());
            if (waiters != null) {
                for (final PendingJoin pending : waiters) {
                    final Player player = Bukkit.getPlayer(pending.playerId());
                    if (player == null) {
                        continue;
                    }
                    CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.RED, "game.not_ready", arenaName));
                    for (final String msg : missing) {
                        CompatProvider.chat().sendMessage(player, this.manager.lang.text(NamedTextColor.GRAY, "game.missing_entry", msg));
                    }
                }
            }
            return;
        }
        final List<PendingJoin> online = waiters != null
                ? waiters.stream().filter(pending -> Bukkit.getPlayer(pending.playerId()) != null).toList()
                : List.of();
        if (online.isEmpty()) {
            this.manager.arenaManager.deleteInstanceWorld(instance.getWorldName());
            return;
        }
        final Game game = new Game(this.manager, instance, this.manager.shopNpcManager, mode);
        this.manager.games.put(this.manager.gameKey(instance), game);
        for (final PendingJoin pending : online) {
            final Player player = Bukkit.getPlayer(pending.playerId());
            if (player != null) {
                game.join(player, pending.teamName(), pending.teleport());
                this.manager.playerGames.put(player.getUniqueId(), game);
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
    void removeFromPendingJoins(final Player player) {
        final UUID playerId = player.getUniqueId();
        for (final List<PendingJoin> queue : this.pendingJoins.values()) {
            queue.removeIf(pending -> pending.playerId().equals(playerId));
        }
        this.pendingJoins.values().removeIf(queue -> queue.isEmpty());
    }

    private static String queueKey(final String arenaName, final @Nullable ArenaMode mode) {
        return arenaName + ":" + (mode != null ? mode.name() : "FREE");
    }

    /**
     * Representa uma entrada pendente aguardando a construção do mundo da arena.
     */
    private record PendingJoin(UUID playerId, @Nullable String teamName, @Nullable ArenaMode mode, boolean teleport) {
    }
}
