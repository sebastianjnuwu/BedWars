package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GamePlayerEliminateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final GamePlayer player;
    private final GamePlayer finalKiller;

    public GamePlayerEliminateEvent(final Game game, final GamePlayer player, @Nullable final GamePlayer finalKiller) {
        this.game = game;
        this.player = player;
        this.finalKiller = finalKiller;
    }

    public @NotNull Game getGame() {
        return this.game;
    }

    public @NotNull GamePlayer getPlayer() {
        return this.player;
    }

    public @Nullable GamePlayer getFinalKiller() {
        return this.finalKiller;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
