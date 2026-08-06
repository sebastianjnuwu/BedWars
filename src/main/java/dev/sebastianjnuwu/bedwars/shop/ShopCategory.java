package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class ShopCategory {

    private final String name;
    private final Material icon;
    private final String displayName;
    private final List<String> lore;
    private final String layoutType;
    private final boolean centered;
    private final List<ShopCategory> children;
    private final List<ShopItem> items;

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

    public String getName() {
        return name;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<ShopCategory> getChildren() {
        return children;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public String getLayoutType() {
        return layoutType;
    }

    public boolean isCentered() {
        return centered;
    }

    public void addChild(ShopCategory child) {
        children.add(child);
    }

    public void addItem(ShopItem item) {
        items.add(item);
    }

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

    public boolean isCategory() {
        return !children.isEmpty();
    }
}
