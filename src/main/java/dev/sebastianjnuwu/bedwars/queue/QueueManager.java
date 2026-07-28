package dev.sebastianjnuwu.bedwars.queue;

import dev.sebastianjnuwu.bedwars.lang.LangManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Gerencia filas de entrada para arenas.
 * <p>
 * O QueueManager permite que jogadores entrem em filas para partidas
 * e alocam automaticamente arenas disponíveis quando há jogadores na fila.
 * </p>
 */
public class QueueManager {

    private final Map<String, Queue<Player>> queues;
    private final Map<Player, String> playerQueues;
    private final LangManager lang;

    /**
     * Cria um novo gerenciador de filas.
     */
    public QueueManager(final LangManager lang) {
        this.queues = new ConcurrentHashMap<>();
        this.playerQueues = new ConcurrentHashMap<>();
        this.lang = lang;
    }

    /**
     * Adiciona um jogador a uma fila de arena.
     *
     * @param player jogador
     * @param arenaName nome da arena
     * @return true se adicionado
     */
    public boolean addToQueue(@NotNull Player player, @NotNull String arenaName) {
        if (playerQueues.containsKey(player)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "queue.already_in_queue"));
            return false;
        }

        queues.computeIfAbsent(arenaName, k -> new ConcurrentLinkedQueue<>()).offer(player);
        playerQueues.put(player, arenaName);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "queue.joined", arenaName));

        // Tenta alocar arena automaticamente
        autoAllocate(arenaName);

        return true;
    }

    /**
     * Remove um jogador de uma fila.
     *
     * @param player jogador
     * @return true se removido
     */
    public boolean removeFromQueue(@NotNull Player player) {
        final String arenaName = playerQueues.remove(player);
        if (arenaName == null) {
            return false;
        }

        final Queue<Player> queue = queues.get(arenaName);
        if (queue != null) {
            queue.remove(player);
            if (queue.isEmpty()) {
                queues.remove(arenaName);
            }
        }

        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "queue.left"));
        return true;
    }

    /**
     * Verifica se um jogador está em uma fila.
     *
     * @param player jogador
     * @return true se estiver em fila
     */
    public boolean isInQueue(@NotNull Player player) {
        return playerQueues.containsKey(player);
    }

    /**
     * Retorna o nome da arena da fila do jogador.
     *
     * @param player jogador
     * @return nome da arena ou null
     */
    public @Nullable String getQueueArena(@NotNull Player player) {
        return playerQueues.get(player);
    }

    /**
     * Retorna o tamanho de uma fila.
     *
     * @param arenaName nome da arena
     * @return tamanho da fila
     */
    public int getQueueSize(@NotNull String arenaName) {
        final Queue<Player> queue = queues.get(arenaName);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Tenta alocar uma arena automaticamente quando há jogadores na fila.
     *
     * @param arenaName nome da arena
     */
    private void autoAllocate(@NotNull String arenaName) {
        // Em implementação futura, isso chamaria o ArenaManager para criar uma instância
        // e teleportar os jogadores da fila
    }

    /**
     * Remove todos os jogadores de uma fila.
     *
     * @param arenaName nome da arena
     */
    public void clearQueue(@NotNull String arenaName) {
        final Queue<Player> queue = queues.remove(arenaName);
        if (queue != null) {
            for (final Player player : queue) {
                playerQueues.remove(player);
                player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "queue.cleared", arenaName));
            }
        }
    }

    /**
     * Remove todas as filas.
     */
    public void clearAll() {
        queues.clear();
        playerQueues.clear();
    }
}
