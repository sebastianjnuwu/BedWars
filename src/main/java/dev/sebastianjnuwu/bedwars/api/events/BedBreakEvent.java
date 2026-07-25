package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a team's bed is broken during a game.
 */
public class BedBreakEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final ArenaTeam team;
    private final Player breaker;

    public BedBreakEvent(final Game game, final ArenaTeam team, final Player breaker) {
        this.game = game;
        this.team = team;
        this.breaker = breaker;
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
