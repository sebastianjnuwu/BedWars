package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class GeneratorSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final ArenaGenerator generator;
    private ItemStack item;
    private boolean cancelled;

    public GeneratorSpawnEvent(final ArenaGenerator generator, final ItemStack item) {
        this.generator = generator;
        this.item = item;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public @NotNull ArenaGenerator getGenerator() {
        return this.generator;
    }

    public @NotNull ItemStack getItem() {
        return this.item;
    }

    public void setItem(final ItemStack item) {
        this.item = item;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
