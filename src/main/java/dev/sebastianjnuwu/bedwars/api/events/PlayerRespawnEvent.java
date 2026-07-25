package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando um jogador renasce após ser eliminado de uma equipe no BedWars.
 * <p>
 * Este evento ocorre quando um jogador é trazido de volta ao jogo após perder seu berço
 * e ser eliminado. O jogador é teleportado para o ponto de spawn de sua equipe
 * e agora está vivo e pronto para participar do jogo novamente.
 * </p>
 * <p><b>Segurança de threads:</b> Este evento é chamado na thread principal do servidor.</p>
 *
 * @author Sebastian J. Nuwu
 * @since 1.0
 */
public class PlayerRespawnEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ArenaTeam team;

    public PlayerRespawnEvent(
            final @NotNull Game game,
            final @NotNull Player player,
            final @NotNull ArenaTeam team
    ) {
        super(game);
        this.player = player;
        this.team = team;
    }

    /**
     * Obtém o jogador que renasceu.
     * <p>
     * O jogador agora está vivo, posicionado no spawn de sua equipe e pode
     * participar do jogo novamente.
     * </p>
     * @return o Player que renasceu
     */
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * Obtém a equipe com a qual o jogador renasce.
     * <p>
     * O jogador volta a sua equipe original e competirá
     * por vitória no BedWars com seus companheiros de equipe.
     * </p>
     * @return a ArenaTeam com a qual o jogador renasce
     */
    public @NotNull ArenaTeam getTeam() {
        return this.team;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
