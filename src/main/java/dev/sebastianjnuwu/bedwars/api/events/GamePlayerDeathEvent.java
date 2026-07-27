package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.DeathCause;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GamePlayerDeathEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer victim;
    private final DeathCause cause;

    public GamePlayerDeathEvent(final Game game, final GamePlayer victim, final DeathCause cause) {
        super(game);
        this.victim = victim;
        this.cause = cause;
    }

    public @NotNull GamePlayer getVictim() {
        return this.victim;
    }

    public @NotNull DeathCause getCause() {
        return this.cause;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
