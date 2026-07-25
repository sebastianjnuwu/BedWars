package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Representa uma partida de jogo de BedWars em andamento.
 * <p>
 * Um objeto Game contém todo o estado e a lógica de uma única partida BedWars,
 * incluindo informações da arena, times, jogadores e progresso do jogo.
 * </p>
 * <p>
 * Esta interface não é thread-safe. As implementações devem gerenciar o acesso concorrente
 * externamente quando necessário.
 * </p>
 * <p><b>Thread safety:</b> Implementações devem garantir segurança de threads se acessada
 * por múltiplas threads.</b>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public interface Game {

    /**
     * Obtém a arena à qual esta partida pertence.
     * <p>
     * A arena contém toda a configuração física e lógica da partida, incluindo
     * terreno, posições de spawn, geradores e outras configurações.
     * </p>
     * @return a Arena desta partida
     */
    Arena getArena();

    /**
     * Obtém o estado atual da partida.
     * <p>
     * O estado indica em que fase o jogo está: WAITING (aguardando jogadores),
     * STARTING (contador regressivo), PLAYING (jogo em andamento) ou ENDING (encerrando).
     * </p>
     * @return o GameState atual da partida
     */
    GameState getState();

    /**
     * Verifica se a equipe está sem berço (bedless).
     * <p>
     * Uma equipe está sem berço quando seu único berço restante foi quebrado.
     * Teams sem berço são automaticamente eliminados do jogo.
     * </p>
     * @param team a ArenaTeam a ser verificada
     * @return true se a equipe estiver sem berço, false caso contrário
     */
    boolean isBedless(ArenaTeam team);

    /**
     * Verifica se a equipe está eliminada do jogo.
     * <p>
     * Uma equipe está eliminada quando perdeu seu berço e todos os seus jogadores
     * foram derrotados. Equipes eliminadas não podem mais competir.
     * </p>
     * @param team a ArenaTeam a ser verificada
     * @return true se a equipe estiver eliminada, false caso contrário
     */
    boolean isEliminated(ArenaTeam team);

    /**
     * Obtém a equipe da qual um jogador particípa.
     * <p>
     * Retorna a equipe specificada na qual o jogador está atualmente participando.
     * Se o jogador não estiver em nenhuma equipe (ex. ainda não atribuído),
     * retorna null.
     * </p>
     * @param player o jogador a ser consultado
     * @return a ArenaTeam do jogador, ou null se não estiver em nenhuma equipe
     */
    @Nullable ArenaTeam getPlayerTeam(Player player);

    /**
     * Obtém os dados do jogador dentro desta partida.
     * <p>
     * Retorna um objeto GamePlayer que contém estatísticas e estado do jogador
     * dentro desta partida específica. Se o jogador não estiver nesta partida,
     * retorna null.
     * </p>
     * @param player o jogador a ser consultado
     * @return o GamePlayer do jogador, ou null se não estiver na partida
     */
    @Nullable GamePlayer getGamePlayer(Player player);

    /**
     * Verifica se um jogador está atualmente jogando nesta partida.
     * <p>
     * Retorna true se o jogador estiver participando ativamente do jogo,
     * false caso contrário (ex. deixou a partida ou está assistindo).
     * </p>
     * @param player o jogador a ser verificado
     * @return true se o jogador estiver jogando, false caso contrário
     */
    boolean isPlaying(Player player);

    /**
     * Obtém o número atual de jogadores nesta partida.
     * <p>
     * Conta todos os jogadores que ainda estão participando da partida,
     * incluindo tanto jogadores vivos quanto eliminados (até renascimento).
     * </p>
     * @return o número de jogadores na partida
     */
    int getPlayerCount();

    /**
     * Obtém todos os GamePlayers participantes desta partida.
     * <p>
     * Retorna uma coleção de objetos GamePlayer representando cada jogador
     * ainda participante da partida. Inclui jogadores vivos e eliminados.
     * </p>
     * @return coleção de GamePlayers na partida
     */
    Collection<GamePlayer> getGamePlayers();
}
