package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Disparado quando um jogo de BedWars termina, seja por uma equipe vencendo
 * ou quando o jogo é interrompido forçadamente. Contém a equipe vencedora
 * (nulo se não houver vencedor).
 *
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class GameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final ArenaTeam winner;

    public GameEndEvent(final Game game, final @Nullable ArenaTeam winner) {
        this.game = game;
        this.winner = winner;
    }

    /**
     * Obtém a instância do jogo BedWars que terminou.
     * <p>
     * O jogo foi encerrado, seja por uma equipe tendo vencido ou por ter sido
     * interrompido manualmente. Esta instância contém o estado final do jogo.
     * </p>
     * @return a instância do jogo que terminou
     */
    public Game getGame() {
        return this.game;
    }

    /**
     * Obtém a equipe vencedora do jogo.
     * <p>
     * Retorna a equipe que venceu o jogo. Se {@code null}, o jogo terminou sem
     * um vencedor claro (por exemplo, interrompido ou sem tempo suficiente).
     * </p>
     * @return a ArenaTeam vencedora, ou null se não houver vencedor
     */
    public @Nullable ArenaTeam getWinner() {
        return this.winner;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
