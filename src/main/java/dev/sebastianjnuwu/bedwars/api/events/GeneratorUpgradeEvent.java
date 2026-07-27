package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.game.Game;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GeneratorUpgradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Game game;
    private final ArenaGenerator generator;
    private final int oldTier;
    private final int newTier;
    private boolean cancelled;

    public GeneratorUpgradeEvent(final Game game, final ArenaGenerator generator, final int oldTier, final int newTier) {
        this.game = game;
        this.generator = generator;
        this.oldTier = oldTier;
        this.newTier = newTier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public @NotNull Game getGame() {
        return this.game;
    }

    public @NotNull ArenaGenerator getGenerator() {
        return this.generator;
    }

    public int getOldTier() {
        return this.oldTier;
    }

    public int getNewTier() {
        return this.newTier;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
