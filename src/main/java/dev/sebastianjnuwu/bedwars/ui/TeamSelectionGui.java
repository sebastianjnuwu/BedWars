package dev.sebastianjnuwu.bedwars.ui;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class TeamSelectionGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Inventory inventory;
    private final Player player;
    private final Arena arena;
    private final LangManager lang;
    private final GameManager gameManager;

    public TeamSelectionGui(final Player player, final Arena arena, final LangManager lang, final GameManager gameManager) {
        this.player = player;
        this.arena = arena;
        this.lang = lang;
        this.gameManager = gameManager;
        this.inventory = Bukkit.createInventory(this, 27, MM.deserialize(this.lang.raw("ui.team_selection.title")));
        this.setupItems();
    }

    private void setupItems() {
        final ItemStack bg = createDarkGlass();
        for (int i = 0; i < 27; i++) {
            this.inventory.setItem(i, bg);
        }

        this.inventory.setItem(26, createItem(
                Material.BARRIER,
                MM.deserialize(this.lang.raw("ui.team_selection.close")),
                List.of()
        ));

        final List<ArenaTeam> teams = this.arena.getTeams();
        final int startSlot = 9 + (9 - Math.min(teams.size(), 9)) / 2;

        for (int i = 0; i < Math.min(teams.size(), 9); i++) {
            this.inventory.setItem(startSlot + i, createTeamItem(teams.get(i)));
        }
    }

    private ItemStack createDarkGlass() {
        final ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeamItem(final ArenaTeam team) {
        return createItem(
                getTeamMaterial(team.getColor()),
                getTeamName(team.getColor()),
                List.of()
        );
    }

    private Component getTeamName(final String color) {
        return switch (color.toLowerCase()) {
            case "red", "vermelho" -> MM.deserialize(this.lang.raw("team.name_red"));
            case "blue", "azul" -> MM.deserialize(this.lang.raw("team.name_blue"));
            case "green", "verde" -> MM.deserialize(this.lang.raw("team.name_green"));
            case "yellow", "amarelo" -> MM.deserialize(this.lang.raw("team.name_yellow"));
            case "purple", "roxo" -> MM.deserialize(this.lang.raw("team.name_purple"));
            case "pink", "rosa" -> MM.deserialize(this.lang.raw("team.name_pink"));
            case "orange", "laranja" -> MM.deserialize(this.lang.raw("team.name_orange"));
            case "cyan", "ciano" -> MM.deserialize(this.lang.raw("team.name_cyan"));
            default -> MM.deserialize(this.lang.raw("team.name_white"));
        };
    }

    private Material getTeamMaterial(final String color) {
        return switch (color.toLowerCase()) {
            case "red", "vermelho" -> Material.RED_WOOL;
            case "blue", "azul" -> Material.BLUE_WOOL;
            case "green", "verde" -> Material.GREEN_WOOL;
            case "yellow", "amarelo" -> Material.YELLOW_WOOL;
            case "purple", "roxo" -> Material.PURPLE_WOOL;
            case "pink", "rosa" -> Material.PINK_WOOL;
            case "orange", "laranja" -> Material.ORANGE_WOOL;
            case "cyan", "ciano" -> Material.CYAN_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private ItemStack createItem(final Material material, final Component name, final List<Component> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public boolean onClick(final InventoryClickEvent event) {
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= this.inventory.getSize()) return false;

        if (slot == 26) {
            this.player.closeInventory();
            return true;
        }

        final List<ArenaTeam> teams = this.arena.getTeams();
        final int startSlot = 9 + (9 - Math.min(teams.size(), 9)) / 2;
        final int index = slot - startSlot;

        if (index >= 0 && index < teams.size()) {
            final ArenaTeam team = teams.get(index);
            final Game game = (Game) this.gameManager.getGame(this.arena.getName());
            if (game != null && game.getGamePlayer(this.player) != null) {
                game.switchTeam(this.player, team.getName());
            } else {
                this.gameManager.joinGame(this.player, this.arena.getName(), team.getName());
            }
            this.player.sendMessage(this.lang.text(NamedTextColor.GREEN, "ui.team_selection.team_selected", team.getName()));
            this.player.closeInventory();
            return true;
        }

        return false;
    }

    public void open() {
        this.player.openInventory(this.inventory);
    }

    @Override
    public org.bukkit.inventory.Inventory getInventory() {
        return this.inventory;
    }
}
