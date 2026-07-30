package dev.sebastianjnuwu.bedwars.ui;

import dev.sebastianjnuwu.bedwars.lang.LangManager;
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

public class ConfirmExitGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Inventory inventory;
    private final Player player;
    private final LangManager lang;

    public ConfirmExitGui(final Player player, final LangManager lang) {
        this.player = player;
        this.lang = lang;
        this.inventory = Bukkit.createInventory(this, 27, MM.deserialize(this.lang.raw("ui.confirm_exit.title")));
        this.setupItems();
    }

    private void setupItems() {
        final ItemStack darkGlass = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            this.inventory.setItem(i, darkGlass);
        }

        this.inventory.setItem(12, createItem(
                Material.LIME_WOOL,
                MM.deserialize(this.lang.raw("ui.confirm_exit.yes_exit")),
                List.of(MM.deserialize(this.lang.raw("ui.confirm_exit.yes_exit_desc")))
        ));

        this.inventory.setItem(14, createItem(
                Material.RED_WOOL,
                MM.deserialize(this.lang.raw("ui.confirm_exit.no_cancel")),
                List.of(MM.deserialize(this.lang.raw("ui.confirm_exit.no_cancel_desc")))
        ));
    }

    private ItemStack createItem(final Material material, final Component name, final List<Component> lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(final Material material, final String name) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    public boolean onClick(final InventoryClickEvent event) {
        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= this.inventory.getSize()) return false;

        switch (slot) {
            case 12 -> {
                this.player.closeInventory();
                this.player.performCommand("bw leave");
                return true;
            }
            case 14 -> {
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
