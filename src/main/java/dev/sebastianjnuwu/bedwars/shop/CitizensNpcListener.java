package dev.sebastianjnuwu.bedwars.shop;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listener agnóstico de interação com NPCs da loja (usado quando o backend
 * ativo é o Citizens).
 */
public class CitizensNpcListener implements Listener {

    private final ShopNpcManager shopNpcManager;

    public CitizensNpcListener(final ShopNpcManager shopNpcManager) {
        this.shopNpcManager = shopNpcManager;
    }

    @EventHandler
    public void onEntityInteract(final PlayerInteractEntityEvent event) {
        if (!this.shopNpcManager.isManagedEntity(event.getRightClicked())) {
            return;
        }
        this.shopNpcManager.openShop(event.getPlayer());
    }
}
