package dev.sebastianjnuwu.bedwars.shop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

import de.oliver.fancynpcs.api.events.NpcInteractEvent;

public class NpcListener implements Listener {

    private final GameManager gameManager;
    private final ShopManager shopManager;
    private final LangManager lang;

    public NpcListener(GameManager gameManager, ShopManager shopManager, LangManager lang) {
        this.gameManager = gameManager;
        this.shopManager = shopManager;
        this.lang = lang;
    }

    @EventHandler
    public void onNpcInteract(NpcInteractEvent event) {
        Player player = event.getPlayer();
        Game game = (Game) gameManager.getPlayerGame(player);

        if (game == null) {
            return;
        }
        if (game.getState() != dev.sebastianjnuwu.bedwars.api.model.GameState.PLAYING) {
            return;
        }

        String npcName = event.getNpc().getData().getName();

        if (npcName.equalsIgnoreCase("shop") || npcName.toLowerCase().startsWith("bw-shop-")) {
            String shopName = game.getArena().getShop();
            if (shopName == null) {
                shopName = "default";
            }
            new ShopGui(gameManager, shopManager, lang, player, game, shopName);
        }
    }
}
