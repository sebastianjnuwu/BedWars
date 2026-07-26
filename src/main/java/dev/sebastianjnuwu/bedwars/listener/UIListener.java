package dev.sebastianjnuwu.bedwars.listener;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.ui.ConfirmExitGui;
import dev.sebastianjnuwu.bedwars.ui.TeamSelectionGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class UIListener implements Listener {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    public UIListener(final ArenaManager arenaManager, final GameManager gameManager) {
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        final Block block = event.getClickedBlock();
        if (block == null) return;

        final Player player = event.getPlayer();

        // Verifica se é uma porta de saída (IRON_DOOR)
        if (block.getType() == Material.IRON_DOOR) {
            final BedWarsPlugin plugin = (BedWarsPlugin) Bukkit.getPluginManager().getPlugin("BedWars");
            final LangManager lang = plugin.getLang();

            // Se o jogador já está em uma partida, abre confirmação de saída
            if (this.gameManager.isInGame(player)) {
                final ConfirmExitGui gui = new ConfirmExitGui(player, lang);
                gui.open();
                event.setCancelled(true);
                return;
            }

            // Se não está em partida, verifica se é uma arena válida e abre seleção de time
            final String worldName = player.getWorld().getName();
            if (!worldName.startsWith("bw_")) return;

            final String arenaName = worldName.substring(3);
            final dev.sebastianjnuwu.bedwars.api.model.Arena arena = this.arenaManager.get(arenaName);
            if (arena != null) {
                final TeamSelectionGui gui = new TeamSelectionGui(player, arena, lang, this.gameManager);
                gui.open();
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) return;

        // Verifica se o clique foi no slot 8 (porta de saída)
        if (event.getRawSlot() == 8) {
            if (event.getInventory().getType() == InventoryType.PLAYER) {
                // Verifica se é o item da porta de saída
                final ItemStack clickedItem = event.getCursor();
                if (clickedItem != null && clickedItem.getType() == Material.IRON_DOOR) {
                    final BedWarsPlugin plugin = (BedWarsPlugin) Bukkit.getPluginManager().getPlugin("BedWars");
                    final LangManager lang = plugin.getLang();

                    if (this.gameManager.isInGame(player)) {
                        final ConfirmExitGui gui = new ConfirmExitGui(player, lang);
                        gui.open();
                        event.setCancelled(true);
                    }
                }
            }
        }
    }
}
