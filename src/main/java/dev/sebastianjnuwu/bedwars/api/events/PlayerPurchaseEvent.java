package dev.sebastianjnuwu.bedwars.api.events;

import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerPurchaseEvent extends GameEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final GamePlayer player;
    private final ItemStack item;
    private final int price;
    private final CurrencyType currency;

    public PlayerPurchaseEvent(final Game game, final GamePlayer player, final ItemStack item, final int price, final CurrencyType currency) {
        super(game);
        this.player = player;
        this.item = item;
        this.price = price;
        this.currency = currency;
    }

    public @NotNull GamePlayer getPlayer() {
        return this.player;
    }

    public @NotNull ItemStack getItem() {
        return this.item;
    }

    public int getPrice() {
        return this.price;
    }

    public @NotNull CurrencyType getCurrency() {
        return this.currency;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
