package dev.sebastianjnuwu.bedwars.ui;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import net.kyori.adventure.text.Component;
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
import java.util.Map;

public class TeamSelectionGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PAGE_SIZE = 27; // 3 linhas de times

    private final Inventory inventory;
    private final Player player;
    private final Arena arena;
    private final LangManager lang;
    private final GameManager gameManager;
    private int currentPage;

    public TeamSelectionGui(final Player player, final Arena arena, final LangManager lang, final GameManager gameManager) {
        this.player = player;
        this.arena = arena;
        this.lang = lang;
        this.gameManager = gameManager;
        this.currentPage = 0;
        this.inventory = Bukkit.createInventory(this, 45, MM.deserialize(this.lang.raw("ui.team_selection.title")));
        this.setupItems();
    }

    private void setupItems() {
        // Fundo escuro
        final ItemStack darkGlass = createDarkGlass();
        for (int i = 0; i < 45; i++) {
            this.inventory.setItem(i, darkGlass);
        }

        // Título do menu
        this.inventory.setItem(4, createItem(
                Material.DIAMOND,
                MM.deserialize(this.lang.raw("ui.team_selection.available_teams")),
                List.of(MM.deserialize(this.lang.raw("ui.team_selection.click_to_join")))
        ));

        // Botão de voltar
        this.inventory.setItem(40, createItem(
                Material.ARROW,
                MM.deserialize(this.lang.raw("ui.team_selection.back")),
                List.of()
        ));

        // Botão de página anterior
        this.inventory.setItem(37, createItem(
                Material.RED_CONCRETE,
                MM.deserialize("<red><b><</b></red>"),
                List.of(MM.deserialize(this.lang.raw("ui.team_selection.prev_page")))
        ));

        // Botão de página próxima
        this.inventory.setItem(43, createItem(
                Material.LIME_CONCRETE,
                MM.deserialize("<green><b>></b></green>"),
                List.of(MM.deserialize(this.lang.raw("ui.team_selection.next_page")))
        ));

        // Botão de página atual
        this.inventory.setItem(49, createItem(
                Material.PAPER,
                MM.deserialize(this.lang.raw("ui.team_selection.page", String.valueOf(this.currentPage + 1))),
                List.of()
        ));

        // Lista de times da página atual
        final List<ArenaTeam> teams = this.arena.getTeams();
        final int minPlayers = this.arena.getMinPlayers();
        final int startIdx = this.currentPage * PAGE_SIZE;
        final int endIdx = Math.min(startIdx + PAGE_SIZE, teams.size());

        for (int i = startIdx; i < endIdx; i++) {
            final ArenaTeam team = teams.get(i);
            this.inventory.setItem(i - startIdx + 1, createTeamItem(team, minPlayers));
        }

        // Atualiza o botão de página
        final int totalPages = (teams.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        this.inventory.setItem(49, createItem(
                Material.PAPER,
                MM.deserialize(this.lang.raw("ui.team_selection.page", String.valueOf(this.currentPage + 1) + "/" + totalPages)),
                List.of()
        ));
    }

    private ItemStack createDarkGlass() {
        final ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTeamItem(final ArenaTeam team, final int minPlayers) {
        final Material material = getTeamMaterial(team.getColor());
        final Component name = getTeamName(team.getColor());

        final Component status;
        if (this.arena.getTeams().size() < 2) {
            status = MM.deserialize(this.lang.raw("ui.team_selection.no_teams_available"));
        } else {
            final int playersInTeam = this.playerCountOnTeam(team.getName());
            if (playersInTeam < minPlayers) {
                status = MM.deserialize(this.lang.raw("ui.team_selection.will_start"));
            } else {
                status = MM.deserialize(this.lang.raw("ui.team_selection.waiting_more"));
            }
        }

        return createItem(material, name, List.of(
                MM.deserialize(this.lang.raw("ui.team_selection.min_players", String.valueOf(minPlayers))),
                MM.deserialize(this.lang.raw("ui.team_selection.in_team", String.valueOf(this.playerCountOnTeam(team.getName())))),
                status
        ));
    }

    private Component getTeamName(final String color) {
        return switch (color.toLowerCase()) {
            case "red", "vermelho" -> MM.deserialize("<red><b>🔴 Vermelho</b></red>");
            case "blue", "azul" -> MM.deserialize("<blue><b>🔵 Azul</b></blue>");
            case "green", "verde" -> MM.deserialize("<green><b>🟢 Verde</b></green>");
            case "yellow", "amarelo" -> MM.deserialize("<yellow><b>🟡 Amarelo</b></yellow>");
            case "purple", "roxo" -> MM.deserialize("<purple><b>🟣 Roxo</b></purple>");
            case "pink", "rosa" -> MM.deserialize("<light_purple><b>🌸 Rosa</b></light_purple>");
            case "orange", "laranja" -> MM.deserialize("<gold><b>🟠 Laranja</b></gold>");
            case "cyan", "ciano" -> MM.deserialize("<aqua><b>🔵 Ciano</b></aqua>");
            default -> MM.deserialize("<white><b>⬜ Branco</b></white>");
        };
    }

    private int playerCountOnTeam(final String teamName) {
        if (this.gameManager == null) return 0;

        final dev.sebastianjnuwu.bedwars.api.model.Game game = this.arena.getName() != null ?
                this.gameManager.getGame(this.arena.getName()) : null;
        if (game == null) return 0;

        int count = 0;
        for (final dev.sebastianjnuwu.bedwars.api.model.GamePlayer gp : game.getGamePlayers()) {
            if (gp.getTeam() != null && gp.getTeam().getName().equalsIgnoreCase(teamName)) {
                count++;
            }
        }
        return count;
    }

    public void nextpage() {
        final List<ArenaTeam> teams = this.arena.getTeams();
        final int totalPages = (teams.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (this.currentPage < totalPages - 1) {
            this.currentPage++;
            this.setupItems();
            this.player.openInventory(this.inventory);
        }
    }

    public void prevPage() {
        if (this.currentPage > 0) {
            this.currentPage--;
            this.setupItems();
            this.player.openInventory(this.inventory);
        }
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

        if (slot == 40) {
            this.player.closeInventory();
            return true;
        }

        // Botão de página anterior
        if (slot == 37) {
            this.prevPage();
            return true;
        }

        // Botão de página próxima
        if (slot == 43) {
            this.nextpage();
            return true;
        }

        // Botão de página atual (info)
        if (slot == 49) {
            return true;
        }

        // Times na página atual
        final int startIdx = this.currentPage * PAGE_SIZE;
        if (slot >= 1 && slot < 1 + PAGE_SIZE) {
            final List<ArenaTeam> teams = this.arena.getTeams();
            final int actualIndex = startIdx + (slot - 1);
            if (actualIndex < teams.size()) {
                final ArenaTeam team = teams.get(actualIndex);
                // Tenta entrar no time
                this.player.performCommand("bw join " + this.arena.getName() + " " + team.getName());
                this.player.closeInventory();
                return true;
            }
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
