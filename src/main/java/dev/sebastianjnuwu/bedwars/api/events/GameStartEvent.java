package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando um jogo de BedWars começa oficialmente e transiciona para o estado PLAYING.
 * <p>
 * Este evento é despachado de forma síncrona após todos os jogadores terem sido teleportados
 * para seus respectivos pontos de spawn da equipe e o jogo ter começado oficialmente.
 * Este evento indica que o jogo está agora na fase ativa PLAYING.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class GameStartEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private boolean cancelled;

    public GameStartEvent(final Game game) {
        this.game = game;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    /**
     * Obtém a instância do jogo BedWars que começou.
     * <p>
     * O jogo está agora no estado PLAYING, com todos os jogadores posicionados em seus
     * spawn da equipe e a contagem regressiva completa.
     * </p>
     * @return o jogo que começou
     */
    public Game getGame() {
        return this.game;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
