package dev.sebastianjnuwu.bedwars.game;

import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Responsável pelos itens, armaduras e cores de time da partida.
 * <p>
 * Centraliza a criação dos itens de saída/seletor de time, a entrega dos
 * itens de spawn e a conversão de cores de time em materiais de lã ou cores
 * de armadura de couro. Métodos estáticos são puros; os de instância usam a
 * língua configurada da partida.
 * </p>
 */
public final class GameItems {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Game game;
    private final LangManager lang;

    /**
     * Cria o gerenciador de itens da partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     * @param lang língua usada nos nomes e descrições (não nula)
     */
    public GameItems(final Game game, final LangManager lang) {
        this.game = game;
        this.lang = lang;
    }

    /**
     * Cria o item de saída da partida (porta de ferro).
     *
     * @return o item de saída
     */
    public ItemStack createExitDoorItem() {
        final ItemStack item = new ItemStack(Material.IRON_DOOR);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.exit_door.name")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.exit_door.lore"))
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Cria o item seletor de time (lã da cor do time).
     *
     * @param team time do jogador ou {@code null}
     * @return o item seletor de time
     */
    public ItemStack createTeamSelectorItem(final ArenaTeam team) {
        final Material material = team != null ? getWoolColor(team.getColor()) : Material.WHITE_WOOL;
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(this.lang.raw("ui.team_selector.name")));
        meta.lore(List.of(
                MM.deserialize(this.lang.raw("ui.team_selector.lore"))
        ));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Dá ao jogador os itens de spawn configurados na arena ({@code spawn_item}).
     * <p>
     * Itens que não couberem no inventário são dropados no chão.
     * </p>
     *
     * @param player jogador que recebe os itens
     */
    public void giveSpawnItems(final Player player) {
        final List<Material> items = this.game.getArena().getSpawnItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (final Material material : items) {
            if (material == null) {
                continue;
            }
            final ItemStack stack = new ItemStack(material);
            final Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
            if (!leftover.isEmpty()) {
                for (final ItemStack drop : leftover.values()) {
                    player.getWorld().dropItem(player.getLocation(), drop);
                }
            }
        }
    }

    /**
     * Converte uma cor de time (ex.: {@code "BLUE"}) no material de lã
     * correspondente.
     *
     * @param dyeColor cor do time ou {@code null}
     * @return material de lã (branco se a cor for desconhecida)
     */
    public static Material getWoolColor(final String dyeColor) {
        if (dyeColor == null) {
            return Material.WHITE_WOOL;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_WOOL;
            case "BLUE", "AZUL" -> Material.BLUE_WOOL;
            case "GREEN", "VERDE" -> Material.GREEN_WOOL;
            case "YELLOW", "AMARELO" -> Material.YELLOW_WOOL;
            case "PURPLE", "ROXO" -> Material.PURPLE_WOOL;
            case "PINK", "ROSA" -> Material.PINK_WOOL;
            case "ORANGE", "LARANJA" -> Material.ORANGE_WOOL;
            case "CYAN", "CIANO" -> Material.CYAN_WOOL;
            case "LIME" -> Material.LIME_WOOL;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_WOOL;
            case "GRAY", "CINZA" -> Material.GRAY_WOOL;
            case "BLACK", "PRETO" -> Material.BLACK_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    /**
     * Converte uma cor de time (ex.: {@code "BLUE"}) na cor de armadura de
     * couro correspondente.
     *
     * @param dyeColor cor do time ou {@code null}
     * @return cor da armadura (branco se a cor for desconhecida)
     */
    public static Color getArmorColor(final String dyeColor) {
        if (dyeColor == null) {
            return Color.WHITE;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Color.fromRGB(255, 85, 85);
            case "BLUE", "AZUL" -> Color.fromRGB(85, 85, 255);
            case "GREEN", "VERDE" -> Color.fromRGB(85, 255, 85);
            case "YELLOW", "AMARELO" -> Color.fromRGB(255, 255, 85);
            case "PURPLE", "ROXO" -> Color.fromRGB(170, 85, 255);
            case "PINK", "ROSA" -> Color.fromRGB(255, 170, 170);
            case "ORANGE", "LARANJA" -> Color.fromRGB(255, 170, 85);
            case "CYAN", "CIANO" -> Color.fromRGB(85, 255, 255);
            case "LIME" -> Color.fromRGB(85, 255, 85);
            case "LIGHT_BLUE", "AZUL_CLARO" -> Color.fromRGB(85, 170, 255);
            case "GRAY", "CINZA" -> Color.fromRGB(170, 170, 170);
            case "BLACK", "PRETO" -> Color.fromRGB(0, 0, 0);
            default -> Color.WHITE;
        };
    }

    private static ItemStack coloredLeather(final Material material, final Color color) {
        final ItemStack item = new ItemStack(material);
        final LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Equipa o jogador com a armadura de couro da cor do time.
     *
     * @param player jogador a ser equipado
     * @param team   time que define a cor da armadura
     */
    public static void applyTeamArmor(final Player player, final ArenaTeam team) {
        if (team == null || team.getColor() == null) {
            return;
        }
        final Color color = getArmorColor(team.getColor());
        player.getInventory().setHelmet(coloredLeather(Material.LEATHER_HELMET, color));
        player.getInventory().setChestplate(coloredLeather(Material.LEATHER_CHESTPLATE, color));
        player.getInventory().setLeggings(coloredLeather(Material.LEATHER_LEGGINGS, color));
        player.getInventory().setBoots(coloredLeather(Material.LEATHER_BOOTS, color));
    }
}
