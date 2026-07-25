package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando uma equipe é eliminada (berço quebrado + todos os jogadores mortos).
 * <p>
 * Este evento ocorre quando uma equipe perde seu berço e todos os seus jogadores estão mortos,
 * resultando na derrota definitiva da equipe e no término do jogo. Este evento é
 * chamado imediatamente após o berço ser quebrado e indica um vencedor do jogo.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class TeamEliminateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final ArenaTeam team;

    public TeamEliminateEvent(final Game game, final ArenaTeam team) {
        this.game = game;
        this.team = team;
    }

    /**
     * Obtém a instância do jogo BedWars no qual a equipe foi eliminada.
     * <p>
     * O jogo pode terminar imediatamente após a eliminação desta equipe, pois
     * restará apenas uma equipe sobrevivente, resultando em uma vitória.
     * </p>
     * @return a instância do jogo
     */
    public Game getGame() {
        return this.game;
    }

    /**
     * Obtém a equipe que foi eliminada.
     * <p>
     * Esta equipe perdeu seu berço e todos os seus jogadores estão mortos,
     * resultando em sua eliminação definitiva da partida. Este evento marca
     * o fim da partida para esta equipe.
     * </p>
     * @return a ArenaTeam eliminada
     */
    public ArenaTeam getTeam() {
        return this.team;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
