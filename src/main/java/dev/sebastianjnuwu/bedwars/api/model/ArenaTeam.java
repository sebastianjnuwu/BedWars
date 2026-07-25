package dev.sebastianjnuwu.bedwars.api.model;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Representa uma equipe em um jogo de BedWars.
 * <p>
 * Uma equipe consiste em jogadores que competem juntos para proteger seu berço
 * e atacar os berços adversários. Cada equipe tem um nome, uma cor e spawned
 * específicos no mapa.
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
public interface ArenaTeam {

    /**
     * Obtém o nome identificador da equipe.
     * <p>
     * Este nome é usado para identificar exclusivamente uma equipe dentro de uma arena.
     * Geralmente corresponde a cores como "RED", "BLUE", "GREEN", etc.
     * </p>
     * @return o nome da equipe
     */
    String getName();

    /**
     * Obtém a cor visual da equipe.
     * <p>
     * Retorna a cor que representa a equipe no mapa. Geralmente usado para
     * distinção visual de jogadores e elementos da equipe.
     * </p>
     * @return a cor da equipe
     */
    String getColor();

    /**
     * Obtém o ponto de spawn onde os jogadores da equipe são teleportados.
     * <p>
     * Os jogadores renascem neste local quando derrotados e é também o ponto de
     * entrada inicial quando o jogo começa.
     * </p>
     * @return o Location do spawn, ou null se não estiver definido
     */
    @Nullable Location getSpawn();

    /**
     * Define o ponto de spawn para a equipe.
     * <p>
     * Os jogadores serão teleportados para este local quando renascem ou entram no jogo.
     * </p>
     * @param spawn o novo Location do spawn
     */
    void setSpawn(Location spawn);

    /**
     * Obtém os dados de bloco do spawn para preservação.
     * <p>
     * Retorna os dados exatos do bloco no local do spawn para garantir
     * que o spawn seja restaurado após reconstrução do mundo.
     * </p>
     * @return os dados do bloco do spawn, ou null se não estiver definido
     */
    @Nullable String getSpawnBlockData();

    /**
     * Define os dados de bloco do spawn para preservação.
     * <p>
     * Armazena os dados do bloco no local do spawn para que possa ser restaurado
     * após reconstrução do mundo.
     * </p>
     * @param spawnBlockData os dados do bloco a serem armazenados
     */
    void setSpawnBlockData(String spawnBlockData);

    /**
     * Obtém o local do berço da equipe.
     * <p>
     * O berço é a cama que a equipe deve proteger. Se o berço for quebrado,
     * a equipe é imediatamente eliminada do jogo.
     * </p>
     * @return o Location do berço, ou null se não estiver definido
     */
    @Nullable Location getBed();

    /**
     * Define o local do berço para a equipe.
     * <p>
     * Os jogadores posicionarão seu berço neste local durante a preparação do jogo.
     * </p>
     * @param bed o novo Location do berço
     */
    void setBed(Location bed);

    /**
     * Obtém a direção que o berço está enfrentando.
     * <p>
     * Indica em que direção o berço está orientado, importante para regras de jogo
     * e lógica de quebra.
     * </p>
     * @return a direção do berço, ou null se não estiver definida
     */
    @Nullable String getBedFacing();

    /**
     * Define a direção que o berço está enfrentando.
     * <p>
     * Define em que direção o berço está orientado para o jogo.
     * </p>
     * @param bedFacing a nova direção do berço
     */
    void setBedFacing(String bedFacing);

    /**
     * Retorna a fornalha (forge) deste time, ou null se não configurada.
     */
    @Nullable ArenaGenerator getForge();

    /**
     * Define a fornalha do time.
     *
     * @param forge o gerador de fornalha, ou null para remover
     */
    void setForge(@Nullable ArenaGenerator forge);
}
