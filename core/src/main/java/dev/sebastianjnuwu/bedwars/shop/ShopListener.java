package dev.sebastianjnuwu.bedwars.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import dev.sebastianjnuwu.bedwars.shop.gui.ShopGui;

public class ShopListener implements Listener {

    private static final int SHOP_SLOTS = 54;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopGui shopGui) {
            shopGui.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof ShopGui)) {
            return;
        }
        for (final int rawSlot : event.getRawSlots()) {
            if (rawSlot < SHOP_SLOTS) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof ShopGui) {
            ShopGui.removeOpenGui(player);
        }
    }
}
