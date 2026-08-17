package dev.sebastianjnuwu.bedwars.shop.gui;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.shop.model.ShopCategory;
import dev.sebastianjnuwu.bedwars.shop.model.ShopItem;

/**
 * Responsável pela renderização da GUI da loja de BedWars.
 * <p>
 * Constrói o título, a borda, a lista de categorias, os produtos da categoria
 * ativa, o posicionamento em grade (linhas/colunas/centralização) e os itens
 * de exibição com o preço e estado do upgrade.
 * </p>
 */
class ShopGuiRenderer {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CATEGORY_SLOTS = 9;

    private final ShopGui gui;
    private final ShopSlotGrid slotGrid;

    ShopGuiRenderer(final ShopGui gui) {
        this.gui = gui;
        this.slotGrid = new ShopSlotGrid(gui);
    }

    void render() {
        final Component title = buildTitle();
        if (!title.equals(this.gui.currentTitleComponent)) {
            this.gui.inventory = Bukkit.createInventory(this.gui, 54, title);
            this.gui.currentTitleComponent = title;
            this.gui.player.openInventory(this.gui.inventory);
        }

        this.gui.inventory.clear();

        fillBorder();

        if (this.gui.currentCategory == null) {
            renderCategoryList();
        } else {
            renderCategoryItems();
        }
    }

    private Component buildTitle() {
        if (this.gui.currentCategory == null) {
            return MM.deserialize(this.gui.baseTitle);
        }
        final String categoryName = this.gui.currentCategory.getDisplayName() != null
                ? this.gui.currentCategory.getDisplayName()
                : this.gui.currentCategory.getName();
        return MM.deserialize(this.gui.baseTitle + " > " + categoryName);
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);

        for (int i = 0; i < 9; i++) {
            this.gui.inventory.setItem(i, border.clone());
        }

        for (int i = 45; i < 54; i++) {
            this.gui.inventory.setItem(i, border.clone());
        }
    }

    private void renderCategoryList() {
        int slot = 0;
        for (ShopCategory cat : this.gui.categories) {
            if (slot >= CATEGORY_SLOTS) {
                break;
            }
            ItemStack icon = cat.createIconItem();
            this.gui.inventory.setItem(slot, icon);
            slot++;
        }
    }

    private void renderCategoryItems() {
        List<Object> entries = new ArrayList<>();
        for (ShopCategory child : this.gui.currentCategory.getChildren()) {
            entries.add(child);
        }
        entries.addAll(this.gui.currentCategory.getItems());

        List<Integer> slots = this.slotGrid.computeSlots(entries, this.gui.currentPage);

        int itemIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            int page = i / ShopSlotGrid.ITEMS_PER_PAGE;
            if (page != this.gui.currentPage) {
                continue;
            }

            Object entry = entries.get(i);
            if (entry instanceof ShopCategory cat) {
                int slot = slots.get(itemIndex++);
                if (slot >= ShopSlotGrid.ITEMS_START) {
                    this.gui.inventory.setItem(slot, cat.createIconItem());
                }
            } else if (entry instanceof ShopItem item) {
                int slot = slots.get(itemIndex++);
                if (slot >= ShopSlotGrid.ITEMS_START) {
                    ItemStack displayStack = createDisplayItem(item);
                    this.gui.inventory.setItem(slot, displayStack);
                }
            }
        }

        int totalPages = (entries.size() + ShopSlotGrid.ITEMS_PER_PAGE - 1) / ShopSlotGrid.ITEMS_PER_PAGE;
        if (totalPages > 1) {
            if (this.gui.currentPage > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prev.getItemMeta();
                prevMeta.displayName(MM.deserialize(this.gui.lang.raw("shop.previous")));
                prev.setItemMeta(prevMeta);
                this.gui.inventory.setItem(45, prev);
            }
            if (this.gui.currentPage < totalPages - 1) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = next.getItemMeta();
                nextMeta.displayName(MM.deserialize(this.gui.lang.raw("shop.next")));
                next.setItemMeta(nextMeta);
                this.gui.inventory.setItem(53, next);
            }
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(MM.deserialize(this.gui.lang.raw("shop.back_categories")));
        back.setItemMeta(backMeta);
        this.gui.inventory.setItem(49, back);
    }

    List<Integer> computeSlots(final List<Object> entries, final int page) {
        return this.slotGrid.computeSlots(entries, page);
    }

    private ItemStack createDisplayItem(ShopItem item) {
        ItemStack stack = item.createItemStack();
        this.gui.applyTeamColor(stack);
        ItemMeta meta = stack.getItemMeta();

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());

        if ("forge".equals(item.getUpgrade())) {
            final ForgeLevel next = this.gui.forgeUpgradeLevel();
            if (next == null || next.upgradeMaterial() == null) {
                stack = new ItemStack(Material.RED_STAINED_GLASS_PANE, stack.getAmount());
                meta = stack.getItemMeta();
                lore.add(MM.deserialize(this.gui.lang.raw("shop.forge_maxed")));
            } else {
                lore.add(MM.deserialize(this.gui.lang.raw("shop.price", String.valueOf(next.upgradePrice()), this.gui.currencyName(next.upgradeMaterial()))));
                lore.add(MM.deserialize(this.gui.lang.raw("shop.forge_next_level", String.valueOf(next.level()))));
            }
        } else if (item.getUpgrade() != null) {
            final ForgeLevel next = this.gui.teamUpgradeLevel(item.getUpgrade());
            if (next == null || next.upgradeMaterial() == null) {
                stack = new ItemStack(Material.RED_STAINED_GLASS_PANE, stack.getAmount());
                meta = stack.getItemMeta();
                lore.add(MM.deserialize(this.gui.lang.raw("shop.upgrade_maxed", this.gui.upgradeName(item.getUpgrade()))));
            } else {
                lore.add(MM.deserialize(this.gui.lang.raw("shop.price", String.valueOf(next.upgradePrice()), this.gui.currencyName(next.upgradeMaterial()))));
                lore.add(MM.deserialize(this.gui.lang.raw("shop.forge_next_level", String.valueOf(next.level()))));
            }
        } else {
            String currencyName = switch (item.getCurrency()) {
                case IRON -> this.gui.lang.raw("shop.currency_iron");
                case GOLD -> this.gui.lang.raw("shop.currency_gold");
                case DIAMOND -> this.gui.lang.raw("shop.currency_diamond");
                case EMERALD -> this.gui.lang.raw("shop.currency_emerald");
            };
            lore.add(MM.deserialize(this.gui.lang.raw("shop.price", String.valueOf(item.getPrice()), currencyName)));
        }

        if (item.getUpgrade() != null) {
            lore.add(MM.deserialize(this.gui.lang.raw("shop.team_upgrade")));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }
}
