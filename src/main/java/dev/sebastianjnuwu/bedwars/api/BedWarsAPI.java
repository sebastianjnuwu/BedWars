package dev.sebastianjnuwu.bedwars.api;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Interface principal da API do BedWars.
 * <p>
 * Fornece acesso a todos os jogos ativos, arenas registradas e operações
 * de controle do plugin. Plugins externos podem obter uma instância através
 * de {@code Bukkit.getServicesManager().load(BedWarsAPI.class)} ou fazendo
 * cast da instância do plugin principal.
 * </p>
 */
public interface BedWarsAPI {

    /**
     * Obtém o jogo ativo para uma arena pelo nome.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param arenaName nome da arena registrada
     * @return o jogo ativo, ou {@code null} se nenhum jogo estiver em andamento
     *         ou a arena não existir
     */
    @Nullable dev.sebastianjnuwu.bedwars.api.model.Game getGame(@NotNull String arenaName);

    /**
     * Obtém o jogo em que um jogador está atualmente.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param player o jogador a ser consultado
     * @return o jogo do jogador, ou {@code null} se não estiver em nenhum jogo
     */
    @Nullable dev.sebastianjnuwu.bedwars.api.model.Game getPlayerGame(@NotNull Player player);

    /**
     * Verifica se um jogador está atualmente em uma partida.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param player o jogador a ser consultado
     * @return {@code true} se o jogador estiver em uma partida
     */
    boolean isInGame(@NotNull Player player);

    /**
     * Obtém os dados do jogador dentro da partida atual.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param player o jogador a ser consultado
     * @return os dados do jogador na partida, ou {@code null} se não estiver
     *         em nenhuma partida
     */
    @Nullable GamePlayer getGamePlayer(@NotNull Player player);

    /**
     * Obtém o gerenciador de arenas para operações de registro e carga.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @return o gerenciador de arenas
     */
    @NotNull dev.sebastianjnuwu.bedwars.api.ArenaManager getArenaManager();

    /**
     * Obtém o gerenciador de jogos para operações de criação e controle.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @return o gerenciador de jogos
     */
    @NotNull dev.sebastianjnuwu.bedwars.api.GameManager getGameManager();

    /**
     * Retorna todas as arenas registradas no plugin.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @return coleção de todas as arenas disponíveis
     */
    @NotNull Collection<Arena> getArenas();

    /**
     * Obtém uma arena registrada pelo nome.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param name nome da arena
     * @return a arena encontrada, ou {@code null} se não existir
     */
    @Nullable Arena getArena(@NotNull String name);

    /**
     * Força o início imediato de uma partida em uma arena,
     * ignorando a contagem regressiva e o número mínimo de jogadores.
     * <p><b>Segurança de threads:</b> Deve ser chamado na thread principal do servidor.</p>
     *
     * @param arenaName nome da arena para iniciar
     * @return {@code true} se a partida foi iniciada com sucesso,
     *         {@code false} se a arena não existir ou já estiver em andamento
     */
    boolean forceStart(@NotNull String arenaName);

    /**
     * Força o término imediato de uma partida em uma arena.
     * <p>
     * O time com mais jogadores vivos será declarado vencedor.
     * Se não houver vencedor, a partida é encerrada sem vencedor.
     * </p>
     * <p><b>Segurança de threads:</b> Deve ser chamado na thread principal do servidor.</p>
     *
     * @param arenaName nome da arena para encerrar
     * @return {@code true} se a partida foi encerrada com sucesso,
     *         {@code false} se a arena não existir ou não houver jogo ativo
     */
    boolean forceEnd(@NotNull String arenaName);

    /**
     * Adiciona um jogador a uma partida em uma arena específica.
     * <p>
     * O jogador será atribuído automaticamente ao time com menos membros.
     * Se o jogo ainda não existir, ele será criado.
     * </p>
     * <p><b>Segurança de threads:</b> Deve ser chamado na thread principal do servidor.</p>
     *
     * @param player    o jogador a ser adicionado
     * @param arenaName nome da arena para entrar
     * @return {@code true} se o jogador entrou na partida com sucesso
     */
    boolean addPlayer(@NotNull Player player, @NotNull String arenaName);

    /**
     * Adiciona um jogador a uma partida em uma arena específica,
     * atribuindo-o a um time específico.
     * <p>
     * Se o jogo ainda não existir, ele será criado.
     * </p>
     * <p><b>Segurança de threads:</b> Deve ser chamado na thread principal do servidor.</p>
     *
     * @param player    o jogador a ser adicionado
     * @param arenaName nome da arena para entrar
     * @param teamName  nome do time para atribuir o jogador
     * @return {@code true} se o jogador entrou na partida com sucesso
     */
    boolean addPlayer(@NotNull Player player, @NotNull String arenaName, @NotNull String teamName);

    /**
     * Remove um jogador da partida atual.
     * <p>
     * Se a partida ficar vazia após a remoção, ela será automaticamente
     * encerrada e removida do gerenciador de jogos.
     * </p>
     * <p><b>Segurança de threads:</b> Deve ser chamado na thread principal do servidor.</p>
     *
     * @param player o jogador a ser removido
     * @return {@code true} se o jogador foi removido com sucesso
     */
    boolean removePlayer(@NotNull Player player);

    /**
     * Obtém todos os jogadores presentes em uma arena específica.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param arenaName nome da arena
     * @return uma coleção de jogadores na arena
     */
    @NotNull Collection<Player> getPlayersInArena(@NotNull String arenaName);

    /**
     * Envia uma mensagem para todos os jogadores na partida da arena.
     * <p><b>Segurança de threads:</b> Pode ser chamado de qualquer thread.</p>
     *
     * @param arenaName nome da arena
     * @param message   mensagem a ser enviada
     */
    void broadcast(@NotNull String arenaName, @NotNull String message);
}
