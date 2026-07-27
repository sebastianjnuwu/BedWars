package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GamePlayerStatChangeEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer player;
    private final StatType stat;
    private final int oldValue;
    private final int newValue;

    public GamePlayerStatChangeEvent(final Game game, final GamePlayer player, final StatType stat, final int oldValue, final int newValue) {
        super(game);
        this.player = player;
        this.stat = stat;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public @NotNull GamePlayer getPlayer() {
        return this.player;
    }

    public @NotNull StatType getStat() {
        return this.stat;
    }

    public int getOldValue() {
        return this.oldValue;
    }

    public int getNewValue() {
        return this.newValue;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
