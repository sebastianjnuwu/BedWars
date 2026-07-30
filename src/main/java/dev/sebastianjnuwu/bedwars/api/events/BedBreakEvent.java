package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Disparado quando o berço de uma equipe é quebrado durante uma partida.
 */
public class BedBreakEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final ArenaTeam team;
    private final Player breaker;
    private boolean cancelled;

    public BedBreakEvent(final Game game, final ArenaTeam team, final Player breaker) {
        this.game = game;
        this.team = team;
        this.breaker = breaker;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public Game getGame() {
        return this.game;
    }

    public ArenaTeam getTeam() {
        return this.team;
    }

    public Player getBreaker() {
        return this.breaker;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
