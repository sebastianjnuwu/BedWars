package dev.sebastianjnuwu.bedwars.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;

public class GameStateChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final GameState oldState;
    private final GameState newState;
    private boolean cancelled;

    public GameStateChangeEvent(final Game game, final GameState oldState, final GameState newState) {
        this.game = game;
        this.oldState = oldState;
        this.newState = newState;
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

    public GameState getOldState() {
        return this.oldState;
    }

    public GameState getNewState() {
        return this.newState;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
