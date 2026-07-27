package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ArenaSaveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final World world;
    private boolean cancelled;

    public ArenaSaveEvent(final Arena arena, final World world) {
        this.arena = arena;
        this.world = world;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public @NotNull Arena getArena() {
        return this.arena;
    }

    public @NotNull World getWorld() {
        return this.world;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
