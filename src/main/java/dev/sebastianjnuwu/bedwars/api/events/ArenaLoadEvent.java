package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ArenaLoadEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Arena arena;
    private final World world;

    public ArenaLoadEvent(final Arena arena, final World world) {
        this.arena = arena;
        this.world = world;
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
