package dev.sebastianjnuwu.bedwars.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Disparado quando um jogador sai de um jogo de BedWars (manualmente ou por desconexão).
 * <p>
 * Este evento ocorre quando um jogador sai voluntariamente de um jogo ativo de BedWars,
 * seja por comando (/bw leave) ou por desconexão do servidor. Este evento pode
 * ser usado para fins de estatística e para saber quem sai do jogo.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class PlayerLeaveGameEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final Player player;

    public PlayerLeaveGameEvent(final Game game, final Player player) {
        this.game = game;
        this.player = player;
    }

    /**
     * Obtém a instância do jogo BedWars do qual o jogador saiu.
     * <p>
     * O jogo pode ainda estar em progresso ou terminar imediatamente após
     * a saída do jogador, dependendo do número de jogadores restantes.
     * </p>
     * @return a instância do jogo
     */
    public Game getGame() {
        return this.game;
    }

    /**
     * Obtém o jogador que saiu do jogo.
     * <p>
     * Este jogador agora deixou a partida BedWars e pode reentrar mais tarde
     * em uma partida diferente. Seu estado de jogador anterior é limpo.
     * </p>
     * @return o Player que saiu
     */
    public Player getPlayer() {
        return this.player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
