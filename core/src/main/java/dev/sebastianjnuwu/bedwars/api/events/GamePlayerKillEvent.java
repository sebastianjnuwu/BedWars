package dev.sebastianjnuwu.bedwars.api.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;

public class GamePlayerKillEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer killer;
    private final GamePlayer victim;

    public GamePlayerKillEvent(final Game game, final GamePlayer killer, final GamePlayer victim) {
        super(game);
        this.killer = killer;
        this.victim = victim;
    }

    public @NotNull GamePlayer getKiller() {
        return this.killer;
    }

    public @NotNull GamePlayer getVictim() {
        return this.victim;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
