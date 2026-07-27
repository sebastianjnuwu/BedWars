package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GamePlayerStreakEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer player;
    private final int streak;

    public GamePlayerStreakEvent(final Game game, final GamePlayer player, final int streak) {
        super(game);
        this.player = player;
        this.streak = streak;
    }

    public @NotNull GamePlayer getPlayer() {
        return this.player;
    }

    public int getStreak() {
        return this.streak;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
