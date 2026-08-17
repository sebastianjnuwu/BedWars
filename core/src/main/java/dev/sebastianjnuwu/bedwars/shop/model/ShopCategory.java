package dev.sebastianjnuwu.bedwars.shop.model;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Representa uma categoria (ou subcategoria) da loja.
 * <p>
 * Agrupa itens e categorias filhas, além de guardar as opções de
 * posicionamento da categoria ({@code layoutType} e {@code centered})
 * usadas na renderização da GUI.
 * </p>
 */
public class ShopCategory {

    private final String name;
    private final Material icon;
    private final String displayName;
    private final List<String> lore;
    private final String layoutType;
    private final boolean centered;
    private final List<ShopCategory> children;
    private final List<ShopItem> items;

    /**
     * Cria uma nova categoria da loja.
     *
     * @param name        nome interno (chave no YAML)
     * @param icon        material do ícone
     * @param displayName nome de exibição (MiniMessage)
     * @param lore        linhas de lore (MiniMessage)
     * @param layoutType  tipo de layout ({@code row} ou {@code column})
     * @param centered    se o conteúdo deve ser centralizado
     */
    public ShopCategory(String name, Material icon, String displayName, List<String> lore,
            String layoutType, boolean centered) {
        this.name = name;
        this.icon = icon;
        this.displayName = displayName;
        this.lore = lore;
        this.layoutType = layoutType;
        this.centered = centered;
        this.children = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    /**
     * Retorna o nome interno da categoria.
     *
     * @return nome da categoria
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna o material do ícone da categoria.
     *
     * @return ícone
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * Retorna o nome de exibição da categoria.
     *
     * @return nome de exibição ou {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna o lore da categoria.
     *
     * @return lore ou {@code null}
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Retorna as categorias filhas.
     *
     * @return categorias filhas
     */
    public List<ShopCategory> getChildren() {
        return children;
    }

    /**
     * Retorna os itens diretos desta categoria.
     *
     * @return itens
     */
    public List<ShopItem> getItems() {
        return items;
    }

    /**
     * Retorna o tipo de layout da categoria ({@code row} ou {@code column}).
     *
     * @return tipo de layout (padrão {@code row})
     */
    public String getLayoutType() {
        return layoutType;
    }

    /**
     * Verifica se o conteúdo da categoria deve ser centralizado.
     *
     * @return {@code true} se centralizado
     */
    public boolean isCentered() {
        return centered;
    }

    /**
     * Adiciona uma categoria filha.
     *
     * @param child categoria filha
     */
    public void addChild(ShopCategory child) {
        children.add(child);
    }

    /**
     * Adiciona um item à categoria.
     *
     * @param item item da loja
     */
    public void addItem(ShopItem item) {
        items.add(item);
    }

    /**
     * Cria o {@link ItemStack} do ícone desta categoria.
     *
     * @return stack com nome e lore configurados
     */
    public ItemStack createIconItem() {
        Material mat = icon != null ? icon : Material.BARRIER;
        ItemStack stack = new ItemStack(mat);
        var meta = stack.getItemMeta();
        if (displayName != null) {
            meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(displayName));
        }
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream()
                    .map(line -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line))
                    .toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Verifica se esta categoria possui subcategorias.
     *
     * @return {@code true} se é uma categoria com filhos
     */
    public boolean isCategory() {
        return !children.isEmpty();
    }
}
