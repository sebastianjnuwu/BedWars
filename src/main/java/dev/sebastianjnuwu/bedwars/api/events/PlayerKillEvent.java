package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando um jogador é assassinado em um jogo de BedWars.
 * <p>
 * Este evento ocorre quando um jogador elimina outro jogador, resultando na morte
 * do jogador eliminado. Este evento é chamado imediatamente após umakill death
 * e contém informações sobre ambos os jogadores e suas equipes respectivas.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class PlayerKillEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player killer;
    private final Player victim;
    private final ArenaTeam killerTeam;
    private final ArenaTeam victimTeam;

    public PlayerKillEvent(
            final @NotNull Game game,
            final @NotNull Player killer,
            final @NotNull Player victim,
            final @NotNull ArenaTeam killerTeam,
            final @NotNull ArenaTeam victimTeam
    ) {
        super(game);
        this.killer = killer;
        this.victim = victim;
        this.killerTeam = killerTeam;
        this.victimTeam = victimTeam;
    }

    /**
     * Obtém o jogador que realizou a morte (assassino).
     * <p>
     * O assassino é o jogador que causou a morte do jogador vítima. Este jogador
     * recebe um kill e avança na classificação do jogo.
     * </p>
     * @return o Player assassino
     */
    public @NotNull Player getKiller() {
        return this.killer;
    }

    /**
     * Obtém o jogador que foi morto (vitima).
     * <p>
     * O jogador morto perde uma vida e é eliminado do jogo. Se este for o seu
     * berço, a equipe também está eliminada (veja o evento TeamEliminateEvent).
     * </p>
     * @return o Player vitima
     */
    public @NotNull Player getVictim() {
        return this.victim;
    }

    /**
     * Obtém a equipe do assassino.
     * <p>
     * A equipe do jogador que realizou a morte. Este jogador agora tem uma morte
     * a menos e continua competindo com sua equipe.
     * </p>
     * @return a ArenaTeam do assassino
     */
    public @NotNull ArenaTeam getKillerTeam() {
        return this.killerTeam;
    }

    /**
     * Obtém a equipe da vítima.
     * <p>
     * A equipe do jogador que foi morto. Se este for o último membro da equipe
     * e seu berço estiver quebrado, a equipe será imediatamente eliminada.
     * </p>
     * @return a ArenaTeam da vítima
     */
    public @NotNull ArenaTeam getVictimTeam() {
        return this.victimTeam;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
