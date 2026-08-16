package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;

/**
 * Representa um item vendido na loja.
 * <p>
 * Suporta itens simples, conjuntos de armadura ({@code armorSet}) e pacotes
 * recursivos ({@code contents}), além de opções de posicionamento na GUI
 * (skip, column, row, linebreak, pagebreak, absolute).
 * </p>
 */
public class ShopItem {

    private final Material material;
    private final List<Material> armorSet;
    private final List<ShopItem> contents;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final Map<String, Integer> enchants;
    private final String tag;
    private final int price;
    private final CurrencyType currency;
    private final String upgrade;
    private final UpgradeConfig upgradeConfig;

    // Positioning
    private final int skip;
    private final Integer column;
    private final Integer row;
    private final String linebreak;
    private final String pagebreak;
    private final Integer absolute;

    private ShopItem(Builder builder) {
        this.material = builder.material;
        this.armorSet = builder.armorSet;
        this.contents = builder.contents;
        this.amount = builder.amount;
        this.displayName = builder.displayName;
        this.lore = builder.lore;
        this.enchants = builder.enchants;
        this.tag = builder.tag;
        this.price = builder.price;
        this.currency = builder.currency;
        this.upgrade = builder.upgrade;
        this.upgradeConfig = builder.upgradeConfig;
        this.skip = builder.skip;
        this.column = builder.column;
        this.row = builder.row;
        this.linebreak = builder.linebreak;
        this.pagebreak = builder.pagebreak;
        this.absolute = builder.absolute;
    }

    /**
     * Retorna o material principal do item (ícone da loja).
     *
     * @return material ou {@code null} se não definido
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * Retorna o conjunto de armadura associado ao item, se houver.
     *
     * @return lista de materiais das peças ou {@code null}
     */
    public List<Material> getArmorSet() {
        return armorSet;
    }

    /**
     * Retorna os itens filhos deste item (pacote recursivo).
     *
     * @return itens contidos ou {@code null}
     */
    public List<ShopItem> getContents() {
        return contents;
    }

    /**
     * Verifica se o item possui filhos a serem entregues na compra.
     *
     * @return {@code true} se há itens contidos
     */
    public boolean hasContents() {
        return contents != null && !contents.isEmpty();
    }

    /**
     * Retorna a quantidade do item.
     *
     * @return quantidade (padrão 1)
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Retorna o nome de exibição do item (formato MiniMessage).
     *
     * @return nome de exibição ou {@code null}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Retorna as linhas de lore do item (formato MiniMessage).
     *
     * @return lore ou {@code null}
     */
    public List<String> getLore() {
        return lore;
    }

    /**
     * Retorna os encantamentos do item.
     *
     * @return mapa de nome do encantamento para nível ou {@code null}
     */
    public Map<String, Integer> getEnchants() {
        return enchants;
    }

    /**
     * Retorna a tag SNBT aplicada ao item.
     *
     * @return tag ou {@code null}
     */
    public String getTag() {
        return tag;
    }

    /**
     * Retorna o preço do item.
     *
     * @return preço em unidades da moeda
     */
    public int getPrice() {
        return price;
    }

    /**
     * Retorna a moeda usada no preço do item.
     *
     * @return moeda (padrão {@link CurrencyType#IRON})
     */
    public CurrencyType getCurrency() {
        return currency;
    }

    /**
     * Retorna o identificador do upgrade do time (ex.: {@code forge}).
     *
     * @return upgrade ou {@code null} se não for um upgrade
     */
    public String getUpgrade() {
        return upgrade;
    }

    /**
     * Retorna a configuração de níveis do upgrade do time, se houver.
     *
     * @return configuração de níveis ou {@code null}
     */
    public UpgradeConfig getUpgradeConfig() {
        return upgradeConfig;
    }

    /**
     * Retorna quantos slots a mais o item deve pular na GUI.
     *
     * @return quantidade de slots a pular
     */
    public int getSkip() {
        return skip;
    }

    /**
     * Retorna a coluna fixa do item na GUI.
     *
     * @return coluna ou {@code null}
     */
    public Integer getColumn() {
        return column;
    }

    /**
     * Retorna a linha fixa do item na GUI.
     *
     * @return linha ou {@code null}
     */
    public Integer getRow() {
        return row;
    }

    /**
     * Retorna a quebra de linha ({@code before}/{@code after}/{@code both}).
     *
     * @return configuração de quebra de linha ou {@code null}
     */
    public String getLinebreak() {
        return linebreak;
    }

    /**
     * Retorna a quebra de página ({@code before}/{@code after}/{@code both}).
     *
     * @return configuração de quebra de página ou {@code null}
     */
    public String getPagebreak() {
        return pagebreak;
    }

    /**
     * Retorna o slot absoluto do item na GUI.
     *
     * @return slot absoluto ou {@code null}
     */
    public Integer getAbsolute() {
        return absolute;
    }

    /**
     * Cria o {@link ItemStack} de exibição do item.
     *
     * @return stack com material, nome, lore e encantamentos configurados
     */
    public ItemStack createItemStack() {
        Material mat = material != null ? material : Material.BARRIER;
        ItemStack stack = new ItemStack(mat, amount);
        applyTag(stack);
        applyDisplayMeta(stack);
        return stack;
    }

    /**
     * Cria os stacks das peças do conjunto de armadura.
     * <p>
     * Se não houver {@code armorSet}, retorna apenas o stack do item.
     * </p>
     *
     * @return lista com as peças do conjunto
     */
    public List<ItemStack> createArmorSetItems() {
        if (armorSet == null) {
            return List.of(createItemStack());
        }
        List<ItemStack> pieces = new ArrayList<>(armorSet.size());
        for (Material mat : armorSet) {
            ItemStack stack = new ItemStack(mat);
            applyDisplayMeta(stack);
            pieces.add(stack);
        }
        return pieces;
    }

    private void applyTag(final ItemStack stack) {
        if (tag == null || tag.isBlank()) {
            return;
        }
        if (applyPotionTag(stack)) {
            return;
        }
        CompatProvider.nbt().modifyItemStack(stack, tag);
    }

    private boolean applyPotionTag(final ItemStack stack) {
        if (stack.getType() != Material.SPLASH_POTION
                && stack.getType() != Material.LINGERING_POTION
                && stack.getType() != Material.POTION
                && stack.getType() != Material.TIPPED_ARROW) {
            return false;
        }
        final int potionIdx = tag.indexOf("potion:");
        if (potionIdx < 0) {
            return false;
        }
        final int valueStart = tag.indexOf('"', potionIdx) + 1;
        final int valueEnd = tag.indexOf('"', valueStart);
        if (valueStart <= 0 || valueEnd < valueStart) {
            return false;
        }
        final String potionKey = tag.substring(valueStart, valueEnd);
        final var meta = (PotionMeta) stack.getItemMeta();
        if (!CompatProvider.potion().applyPotionType(meta, potionKey)) {
            return false;
        }
        stack.setItemMeta(meta);
        return true;
    }

    private void applyDisplayMeta(ItemStack stack) {
        if (displayName != null || lore != null || (enchants != null && !enchants.isEmpty())) {
            var meta = stack.getItemMeta();
            if (displayName != null) {
                meta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(displayName));
            }
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(line))
                        .toList());
            }
            if (enchants != null) {
                for (var entry : enchants.entrySet()) {
                    var enchant = CompatProvider.registry().getEnchantment(entry.getKey());
                    if (enchant != null) {
                        meta.addEnchant(enchant, entry.getValue(), true);
                    }
                }
            }
            stack.setItemMeta(meta);
        }
    }

    /**
     * Construtor fluente de {@link ShopItem}.
     */
    public static class Builder {
        private Material material;
        private List<Material> armorSet;
        private List<ShopItem> contents;
        private int amount = 1;
        private String displayName;
        private List<String> lore;
        private Map<String, Integer> enchants;
        private String tag;
        private int price;
        private CurrencyType currency;
        private String upgrade;
        private UpgradeConfig upgradeConfig;
        private int skip;
        private Integer column;
        private Integer row;
        private String linebreak;
        private String pagebreak;
        private Integer absolute;

        public Builder material(Material material) {
            this.material = material;
            return this;
        }

        public Builder armorSet(List<Material> armorSet) {
            this.armorSet = armorSet;
            return this;
        }

        public Builder contents(List<ShopItem> contents) {
            this.contents = contents;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder enchants(Map<String, Integer> enchants) {
            this.enchants = enchants;
            return this;
        }

        public Builder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Builder price(int price) {
            this.price = price;
            return this;
        }

        public Builder currency(CurrencyType currency) {
            this.currency = currency;
            return this;
        }

        public Builder upgrade(String upgrade) {
            this.upgrade = upgrade;
            return this;
        }

        public Builder upgradeConfig(UpgradeConfig upgradeConfig) {
            this.upgradeConfig = upgradeConfig;
            return this;
        }

        public Builder skip(int skip) {
            this.skip = skip;
            return this;
        }

        public Builder column(Integer column) {
            this.column = column;
            return this;
        }

        public Builder row(Integer row) {
            this.row = row;
            return this;
        }

        public Builder linebreak(String linebreak) {
            this.linebreak = linebreak;
            return this;
        }

        public Builder pagebreak(String pagebreak) {
            this.pagebreak = pagebreak;
            return this;
        }

        public Builder absolute(Integer absolute) {
            this.absolute = absolute;
            return this;
        }

        /**
         * Constrói o {@link ShopItem} final.
         *
         * @return item construído
         */
        public ShopItem build() {
            return new ShopItem(this);
        }
    }
}
