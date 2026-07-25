package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando um jogador entra em um jogo ativo de BedWars.
 * <p>
 * Este evento ocorre quando um jogador entra em um jogo de BedWars que está atualmente
 * em progresso ou acabou de começar. O jogador é atribuído a uma equipe e posicionado
 * no local de spawn de sua equipe.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class PlayerJoinGameEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final Player player;

    public PlayerJoinGameEvent(final Game game, final Player player) {
        this.game = game;
        this.player = player;
    }

    /**
     * Obtém a instância do jogo BedWars ao qual o jogador entrou.
     * <p>
     * Esta representa a sessão de jogo ativa na qual o jogador acabou de entrar.
     * O estado do jogo pode ser WAITING, STARTING ou PLAYING dependendo de quando
     * o jogador entra.
     * </p>
     * @return a instância do jogo
     */
    public Game getGame() {
        return this.game;
    }

    /**
     * Obtém o jogador que entrou no jogo.
     * <p>
     * O jogador agora faz parte da partida BedWars e participará
     * no jogo até ser eliminado ou o jogo terminar.
     * </p>
     * @return o Player que entrou
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
