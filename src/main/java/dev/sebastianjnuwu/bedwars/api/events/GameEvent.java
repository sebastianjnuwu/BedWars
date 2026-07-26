package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Classe base para todos os eventos de estado do jogo de BedWars.
 * <p>
 * Esta classe de evento abstrata serve como a fundação para todos os eventos
 * relacionados ao jogo em BedWars, fornecendo acesso à instância {@link Game} associada.
 * </p>
 * <p>
 * Eventos específicos do jogo estendem esta classe para participar do sistema de eventos
 * do jogo. Estes eventos são despachados pelo plugin BedWars quando mudanças relevantes
 * no estado do jogo ocorrem.
 * </p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public abstract class GameEvent extends Event implements Cancellable {

    private final Game game;
    private boolean cancelled;

    /**
     * Cria um novo evento de jogo.
     * <p>
     * Eventos de jogo são instanciados pelo motor de jogo do BedWars quando mudanças relevantes
     * no estado do jogo ocorrem.
     * </p>
     * @param game a instância do jogo BedWars associada a este evento
     */
    public GameEvent(final Game game) {
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
     * Obtém a instância do jogo BedWars a qual este evento pertence.
     * <p>
     * O jogo contém todas as informações de estado, incluindo arena, equipes,
     * dados dos jogadores e fase atual do jogo.
     * </p>
     * @return a instância do jogo
     */
    public Game getGame() {
        return this.game;
    }

    @Override
    public abstract @NotNull HandlerList getHandlers();
}
