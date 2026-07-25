package dev.sebastianjnuwu.bedwars.game;

/**
 * Representa os estados possíveis de uma partida no BedWars.
 * <p>
 * O ciclo de vida de uma partida segue a ordem: {@link #WAITING} → {@link #STARTING} → {@link #PLAYING} → {@link #ENDING}.
 * Esta enumeração é thread-safe por ser imutável.
 * </p>
 *
 * @author SebastianJnuwu
 */
public enum GameState {
    /**
     * Estado de espera: jogadores entram na sala e a partida aguarda o número mínimo de jogadores.
     * Blocos e danos estão protegidos neste estado.
     */
    WAITING,
    /**
     * Estado de início: contagem regressiva antes do início da partida.
     * Jogadores não podem quebrar blocos ou causar dano uns aos outros.
     */
    STARTING,
    /**
     * Estado de jogo: a partida está ativa.
     * Jogadores podem quebrar camas, eliminar adversários e coletar recursos.
     */
    PLAYING,
    /**
     * Estado de encerramento: a partida terminou.
     * Estatísticas finais são exibidas e os jogadores são redirecionados ao lobby.
     */
    ENDING
}
