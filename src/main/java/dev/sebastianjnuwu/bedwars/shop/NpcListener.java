package dev.sebastianjnuwu.bedwars.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

public class NpcListener implements Listener {

    private final GameManager gameManager;
    private final ShopManager shopManager;
    private final LangManager lang;
    private final ShopNpcManager shopNpcManager;

    public NpcListener(final GameManager gameManager, final ShopManager shopManager, final LangManager lang) {
        this.gameManager = gameManager;
        this.shopManager = shopManager;
        this.lang = lang;
        this.shopNpcManager = gameManager.getShopNpcManager();
    }

    @EventHandler
    public void onEntityInteract(final PlayerInteractEntityEvent event) {
        if (!this.shopNpcManager.isManagedEntity(event.getRightClicked())) {
            return;
        }
        this.handleInteract(event.getPlayer());
    }

    private void handleInteract(final Player player) {
        final Game game = (Game) this.gameManager.getPlayerGame(player);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }

        String shopName = game.getArena().getShop();
        if (shopName == null) {
            shopName = "default";
        }
        new ShopGui(this.gameManager, this.shopManager, this.lang, player, game, shopName);
    }
}
