package dev.sebastianjnuwu.bedwars.shop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import dev.sebastianjnuwu.bedwars.manager.game.GameManager;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;

public class NpcListener implements Listener {

    private final ShopNpcManager shopNpcManager;

    public NpcListener(final GameManager gameManager) {
        this.shopNpcManager = gameManager.getShopNpcManager();
    }

    @EventHandler
    public void onEntityInteract(final PlayerInteractEntityEvent event) {
        if (!this.shopNpcManager.isManagedEntity(event.getRightClicked())) {
            return;
        }
        this.shopNpcManager.openShop(event.getPlayer());
    }

    @EventHandler
    public void onNpcInteract(final NpcInteractEvent event) {
        if (!this.shopNpcManager.isManagedNpc(event.getNpc())) {
            return;
        }
        this.shopNpcManager.openShop(event.getPlayer());
    }
}
