package dev.sebastianjnuwu.bedwars.api.model;

/**
 * Representa os diferentes estados de uma partida de BedWars.
 * <p>
 * Estes estados definem em que fase o jogo está atualmente e afetam
 * quais ações podem ser executadas pelos jogadores e o plugin.
 * </p>
 * <p>
 * Os estados seguem uma progressão lógica:
 * <ul>
 *   <li>WAITING: Jogo não iniciado, jogadores podem entrar</li>
 *   <li>STARTING: Contador regressivo iniciado, jogadores não podem sair</li>
 *   <li>PLAYING: Jogo em andamento, jogadores competem</li>
 *   <li>ENDING: Jogo terminou, resultados exibidos</li>
 * </ul>
 * </p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public enum GameState {
    /**
     * Estado onde o jogo não foi iniciado.
     * <p>
     * Neste estado, jogadores podem entrar/sair livremente e a contagem
     * regressiva não foi iniciada. Este é o estado padrão de uma nova arena.
     * </p>
     */
    WAITING,
    /**
     * Estado onde o jogo iniciou a contagem regressiva.
     * <p>
     * Neste estado, a contagem regressiva está em progresso e jogadores
     * não podem sair da partida. O jogo iniciará após a contagem regressiva
     * terminar (ou forceStart ser chamado).
     * </p>
     */
    STARTING,
    /**
     * Estado onde o jogo está ativamente em progresso.
     * <p>
     * Neste estado, jogadores competem ativamente, enderando mobs,
     * atacando outros jogadores e tentando ser a última equipe survivente.
     * </p>
     */
    PLAYING,
    /**
     * Estado onde o jogo terminou mas resultados estão sendo exibidos.
     * <p>
     * Neste estado, o jogo terminou e os resultados estão sendo exibidos
     * aos jogadores. Após um timeout, o estado geralmente volta para WAITING.
     * </p>
     */
    ENDING
}
