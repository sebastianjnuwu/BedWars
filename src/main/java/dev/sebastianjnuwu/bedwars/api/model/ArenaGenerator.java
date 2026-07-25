package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Representa um gerador em uma arena de BedWars.
 * <p>
 * Geradores são estruturas que adicionam elementos ao mapa do jogo, como,
 * cofres, pódios de produtores, portais ou outros blocos funcionais que
 * influenciam a jogabilidade.
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
public interface ArenaGenerator {

    /**
     * Obtém o tipo do gerador.
     * <p>
     * Define que tipo de bloco funcional este gerador cria, como "CHEST",
     * "BREAKER", "PORTER", etc. Este tipo determina o comportamento do gerador.
     * </p>
     * @return o tipo do gerador
     */
    String getType();

    /**
     * Obtém a localização deste gerador no mapa.
     * <p>
     * Define onde o gerador está posicionado no mundo do jogo. Os jogadores
     * podem interagir com este gerador baseado nesta localização.
     * </p>
     * @return a Location do gerador
     */
    Location getLocation();

    /**
     * Obtém os dados do bloco de origem para preservação.
     * <p>
     * Retorna os dados exatos do bloco no local de origem do gerador para
     * garantir que possa ser restaurado após reconstrução do mundo.
     * </p>
     * @return os dados do bloco de origem, ou null se não estiverem definidos
     */
    @Nullable String getOriginBlockData();

    /**
     * Define os dados do bloco de origem para preservação.
     * <p>
     * Armazena os dados do bloco no local de origem do gerador para que possa
     * ser restaurado após reconstrução do mundo.
     * </p>
     * @param originBlockData os dados do bloco a serem armazenados
     */
    void setOriginBlockData(String originBlockData);

    /**
     * Obtém os dados do bloco acima da origem para preservação.
     * <p>
     * Retorna os dados do bloco diretamente acima do local de origem do gerador
     * para preservação do mundo.
     * </p>
     * @return os dados do bloco acima da origem, ou null se não estiverem definidos
     */
    @Nullable String getOriginBlockDataAbove();

    /**
     * Define os dados do bloco acima da origem para preservação.
     * <p>
     * Armazena os dados do bloco diretamente acima do local de origem do gerador
     * para preservação do mundo.
     * </p>
     * @param originBlockDataAbove os dados do bloco a serem armazenados
     */
    void setOriginBlockDataAbove(String originBlockDataAbove);

    /**
     * Obtém o time vinculado ao gerador (apenas fornalha).
     *
     * @return nome do time ou null para geradores globais
     */
    @Nullable String getTeam();

    /**
     * Define o time vinculado ao gerador (apenas fornalha).
     *
     * @param team nome do time
     */
    void setTeam(@Nullable String team);
}
