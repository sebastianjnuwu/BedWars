package dev.sebastianjnuwu.bedwars.arena;

/**
 * Estados de uma instância de arena.
 */
public enum ArenaState {
    /**
     * Arena criada mas ainda não carregada.
     */
    OFFLINE,

    /**
     * Arena carregada e pronta para partidas.
     */
    READY,

    /**
     * Arena em processo de carregamento.
     */
    LOADING,

    /**
     * Arena com partida em andamento.
     */
    PLAYING,

    /**
     * Arena em processo de reset/reinicialização.
     */
    RESETTING,

    /**
     * Arena com partida aguardando início (countdown).
     */
    STARTING,

    /**
     * Arena com partida finalizando.
     */
    ENDING
}
