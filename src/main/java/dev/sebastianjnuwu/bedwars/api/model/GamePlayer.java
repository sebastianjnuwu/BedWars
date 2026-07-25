package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * Representa dados de um jogador dentro de uma partida de BedWars.
 * <p>
 * Um objeto GamePlayer contém estatísticas e estado de um único jogador
 * dentro de uma partida específica, incluindo kills, mortes, equipe e se está
 * vivo ou eliminado.
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
public interface GamePlayer {

    /**
     * Obtém o UUID único do jogador.
     * <p>
     * O UUID identifica exclusivamente um jogador no servidor Minecraft,
     * permitindo que o plugin associe dados mesmo se o nome do jogador mudar.
     * </p>
     * @return o UUID do jogador
     */
    UUID getUuid();

    /**
     * Obtém a equipe da qual o jogador participa na partida.
     * <p>
     * Retorna a ArenaTeam que o jogador está atualmente participando.
     * Este valor não muda durante o jogo, a menos que o jogador reentre em uma
     * nova partida ou seja manualmente realocado.
     * </p>
     * @return a ArenaTeam do jogador
     */
    ArenaTeam getTeam();

    /**
     * Verifica se o jogador está vivo na partida.
     * <p>
     * Um jogador vivo pode participar do jogo e atacar outros jogadores.
     * Jogadores mortos não podem atacar mas podem renascer se a equipe
     * ainda tiver tempo de renascimento.
     * </p>
     * @return true se o jogador estiver vivo, false se estiver morto/eliminado
     */
    boolean isAlive();

    /**
     * Define o estado de vida do jogador.
     * <p>
     * Altera o estado de vida do jogador. Geralmente chamado quando um jogador
     * é derrotado (setAlive(false)) ou renasce (setAlive(true)).
     * </p>
     * @param alive o novo estado de vida do jogador
     */
    void setAlive(boolean alive);

    /**
     * Obtém o número de mortes do jogador nesta partida.
     * <p>
     * Conta o número de vezes que o jogador foi derrotado nesta partida específica.
     * Este contador é reiniciado quando a partida termina e um novo jogo começa.
     * </p>
     * @return o número de mortes do jogador
     */
    int getDeaths();

    /**
     * Adiciona uma morte ao contador do jogador.
     * <p>
     * Incrementa o contador de mortes do jogador. Geralmente chamado quando um
     * jogador é derrotado e eliminado da partida.
     * </p>
     */
    void addDeath();

    /**
     * Obtém o número de kills do jogador nesta partida.
     * <p>
     * Conta o número de jogadores eliminados pelo jogador nesta partida.
     * O killカウント é usado para determinar a classificação dos jogadores.
     * </p>
     * @return o número de kills do jogador
     */
    int getKills();

    /**
     * Adiciona um kill ao contador do jogador.
     * <p>
     * Incrementa o contador de kills do jogador. Geralmente chamado quando um
     * jogador elimina outro jogador na partida.
     * </p>
     */
    void addKill();
}
