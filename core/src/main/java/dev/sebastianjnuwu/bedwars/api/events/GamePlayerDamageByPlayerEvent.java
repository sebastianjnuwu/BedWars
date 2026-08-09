package dev.sebastianjnuwu.bedwars.api.events;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;

public class GamePlayerDamageByPlayerEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer damager;
    private final GamePlayer victim;
    private double damage;

    public GamePlayerDamageByPlayerEvent(final Game game, final GamePlayer damager, final GamePlayer victim, final double damage) {
        super(game);
        this.damager = damager;
        this.victim = victim;
        this.damage = damage;
    }

    public @NotNull GamePlayer getDamager() {
        return this.damager;
    }

    public @NotNull GamePlayer getVictim() {
        return this.victim;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(final double damage) {
        this.damage = damage;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
