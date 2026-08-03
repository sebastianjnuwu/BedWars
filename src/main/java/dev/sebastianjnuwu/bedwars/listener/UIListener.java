package dev.sebastianjnuwu.bedwars.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.ui.ConfirmExitGui;
import dev.sebastianjnuwu.bedwars.ui.TeamSelectionGui;

public class UIListener implements Listener {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;

    public UIListener(final ArenaManager arenaManager, final GameManager gameManager) {
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        final Block block = event.getClickedBlock();
        final Player player = event.getPlayer();

        final BedWarsPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(BedWarsPlugin.class);
        final LangManager lang = plugin.getLang();

        if (this.gameManager.isInGame(player)) {
            final ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null) {
                return;
            }

            if (hand.getType().name().endsWith("_WOOL") || hand.getType() == Material.COMPASS) {
                final Game game = (Game) this.gameManager.getPlayerGame(player);
                if (game == null) {
                    return;
                }
                final var arena = game.getArena();
                final TeamSelectionGui gui = new TeamSelectionGui(player, arena, lang, this.gameManager);
                gui.open();
                event.setCancelled(true);
                return;
            }

            if (hand.getType() == Material.IRON_DOOR) {
                final ConfirmExitGui gui = new ConfirmExitGui(player, lang);
                gui.open();
                event.setCancelled(true);
                return;
            }
        }

        // Interação com bloco de porta de ferro (entrar na arena pelo lobby ou sair)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null && block.getType() == Material.IRON_DOOR) {
            if (this.gameManager.isInGame(player)) {
                final ConfirmExitGui gui = new ConfirmExitGui(player, lang);
                gui.open();
                event.setCancelled(true);
                return;
            }

            final String worldName = player.getWorld().getName();
            if (!worldName.startsWith("bw_")) {
                return;
            }

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
        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }

        // Delega para o GUI de confirmação de saída
        if (event.getView().getTopInventory().getHolder() instanceof final ConfirmExitGui confirmGui) {
            event.setCancelled(true);
            confirmGui.onClick(event);
            return;
        }

        // Delega para o GUI de seleção de time
        if (event.getView().getTopInventory().getHolder() instanceof final TeamSelectionGui teamGui) {
            event.setCancelled(true);
            teamGui.onClick(event);
            return;
        }

        // Bloqueia interacao com slots de armadura durante lobby (WAITING/STARTING)
        if (event.getView().getBottomInventory().equals(event.getClickedInventory())
                && event.getSlot() >= 36 && event.getSlot() <= 39) {
            if (this.gameManager.isInGame(player)) {
                final Game game = (Game) this.gameManager.getPlayerGame(player);
                if (game != null && game.getState() != GameState.PLAYING) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Clique no slot 8 (porta de saída no inventário do jogador)
        if (event.getRawSlot() == 8) {
            if (event.getInventory().getType() == InventoryType.PLAYER) {
                final ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.IRON_DOOR) {
                    final BedWarsPlugin plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(BedWarsPlugin.class);
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
