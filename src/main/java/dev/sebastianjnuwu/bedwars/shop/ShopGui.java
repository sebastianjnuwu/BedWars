package dev.sebastianjnuwu.bedwars.shop;

import dev.sebastianjnuwu.bedwars.api.events.PlayerPurchaseEvent;
import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShopGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CATEGORY_SLOTS = 9;
    private static final int ITEMS_START = 9;
    private static final int ITEMS_END = 44;
    private static final int ITEMS_PER_PAGE = 36;

    private final GameManager gameManager;
    private final ShopManager shopManager;
    private final LangManager lang;
    private final Player player;
    private final Game game;
    private final String shopName;

    private final Inventory inventory;
    private final List<ShopCategory> categories;
    private ShopCategory currentCategory;
    private int currentPage;

    private static final Map<UUID, ShopGui> openGuis = new HashMap<>();

    public ShopGui(GameManager gameManager, ShopManager shopManager, LangManager lang, Player player, Game game, String shopName) {
        this.gameManager = gameManager;
        this.shopManager = shopManager;
        this.lang = lang;
        this.player = player;
        this.game = game;
        this.shopName = shopName;
        this.categories = shopManager.getCategories(shopName);
        this.currentPage = 0;

        this.inventory = Bukkit.createInventory(this, 54, MM.deserialize(this.lang.raw("shop.title")));

        openGuis.put(player.getUniqueId(), this);
        openMain();
        player.openInventory(inventory);
    }

    public static ShopGui getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    public static void removeOpenGui(Player player) {
        openGuis.remove(player.getUniqueId());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void openMain() {
        this.currentCategory = null;
        this.currentPage = 0;
        render();
    }

    public void openCategory(ShopCategory category) {
        this.currentCategory = category;
        this.currentPage = 0;
        render();
    }

    private void render() {
        inventory.clear();

        fillBorder();

        if (currentCategory == null) {
            renderCategoryList();
        } else {
            renderCategoryItems();
        }
    }

    private void fillBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.displayName(Component.text(" "));
        border.setItemMeta(meta);

        // Top row (category row)
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border.clone());
        }

        // Bottom row
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, border.clone());
        }
    }

    private void renderCategoryList() {
        int slot = 0;
        for (ShopCategory cat : categories) {
            if (slot >= CATEGORY_SLOTS) break;
            ItemStack icon = cat.createIconItem();
            inventory.setItem(slot, icon);
            slot++;
        }
    }

    private void renderCategoryItems() {
        List<Object> entries = new ArrayList<>();
        for (ShopCategory child : currentCategory.getChildren()) {
            entries.add(child);
        }
        entries.addAll(currentCategory.getItems());

        // Show category navigation in top row
        int catSlot = 0;
        for (ShopCategory cat : categories) {
            if (catSlot >= CATEGORY_SLOTS) break;
            ItemStack icon = cat.createIconItem();
            // Mark the current category as selected
            if (cat == currentCategory) {
                var meta = icon.getItemMeta();
                meta.lore(List.of(MM.deserialize(this.lang.raw("shop.selected"))));
                icon.setItemMeta(meta);
            }
            inventory.setItem(catSlot, icon);
            catSlot++;
        }
        // Fill remaining top slots with border
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        for (int i = catSlot; i < CATEGORY_SLOTS; i++) {
            inventory.setItem(i, border.clone());
        }

        List<Integer> slots = computeSlots(entries, currentPage);

        int itemIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            int page = i / ITEMS_PER_PAGE;
            if (page != currentPage) continue;

            Object entry = entries.get(i);
            if (entry instanceof ShopCategory cat) {
                int slot = slots.get(itemIndex++);
                inventory.setItem(slot, cat.createIconItem());
            } else if (entry instanceof ShopItem item) {
                int slot = slots.get(itemIndex++);
                ItemStack displayStack = createDisplayItem(item);
                inventory.setItem(slot, displayStack);
            }
        }

        // Page navigation
        int totalPages = (entries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        if (totalPages > 1) {
            if (currentPage > 0) {
                ItemStack prev = new ItemStack(Material.ARROW);
                ItemMeta prevMeta = prev.getItemMeta();
                prevMeta.displayName(MM.deserialize(this.lang.raw("shop.previous")));
                prev.setItemMeta(prevMeta);
                inventory.setItem(45, prev);
            }
            if (currentPage < totalPages - 1) {
                ItemStack next = new ItemStack(Material.ARROW);
                ItemMeta nextMeta = next.getItemMeta();
                nextMeta.displayName(MM.deserialize(this.lang.raw("shop.next")));
                next.setItemMeta(nextMeta);
                inventory.setItem(53, next);
            }
        }

        // Back button (goes to main category list, not to parent)
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(MM.deserialize(this.lang.raw("shop.back_categories")));
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    private List<Integer> computeSlots(List<Object> entries, int page) {
        List<Integer> slots = new ArrayList<>();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());

        int slotIndex = ITEMS_START;
        int row = 0;
        int col = 0;

        for (int i = startIndex; i < endIndex; i++) {
            Object entry = entries.get(i);

            if (entry instanceof ShopItem item) {
                if (item.getAbsolute() != null) {
                    slots.add(item.getAbsolute());
                    continue;
                }
                if (item.getLinebreak() != null) {
                    switch (item.getLinebreak().toLowerCase()) {
                        case "before":
                            row++;
                            col = 0;
                            slotIndex = ITEMS_START + row * 9;
                            break;
                        case "after":
                            slots.add(slotIndex++);
                            row++;
                            col = 0;
                            slotIndex = ITEMS_START + row * 9;
                            continue;
                        case "both":
                            row++;
                            col = 0;
                            slotIndex = ITEMS_START + row * 9;
                            slots.add(slotIndex++);
                            row++;
                            col = 0;
                            slotIndex = ITEMS_START + row * 9;
                            continue;
                    }
                }
                if (item.getPagebreak() != null) {
                    continue;
                }
                if (item.getColumn() != null) {
                    // Use absolute column within the current row
                    int targetSlot = ITEMS_START + row * 9 + item.getColumn();
                    if (targetSlot < ITEMS_END) {
                        slots.add(targetSlot);
                        slotIndex = targetSlot + 1;
                        col = item.getColumn() + 1;
                        continue;
                    }
                }
                if (item.getRow() != null) {
                    row = item.getRow() - 1;
                    col = 0;
                    slotIndex = ITEMS_START + row * 9;
                }
                if (item.getSkip() > 0) {
                    slotIndex += item.getSkip();
                    col += item.getSkip();
                }
            }

            if (slotIndex > ITEMS_END) {
                row++;
                col = 0;
                slotIndex = ITEMS_START + row * 9;
            }

            if (slotIndex <= ITEMS_END) {
                slots.add(slotIndex);
                slotIndex++;
                col++;
                if (col >= 9) {
                    row++;
                    col = 0;
                    slotIndex = ITEMS_START + row * 9;
                }
            }
        }

        return slots;
    }

    private ItemStack createDisplayItem(ShopItem item) {
        ItemStack stack = item.createItemStack();
        ItemMeta meta = stack.getItemMeta();

        // Add price lore
        String currencyName = switch (item.getCurrency()) {
            case IRON -> this.lang.raw("shop.currency_iron");
            case GOLD -> this.lang.raw("shop.currency_gold");
            case DIAMOND -> this.lang.raw("shop.currency_diamond");
            case EMERALD -> this.lang.raw("shop.currency_emerald");
        };

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(MM.deserialize(this.lang.raw("shop.price", String.valueOf(item.getPrice()), currencyName)));

        if (item.getUpgrade() != null) {
            lore.add(MM.deserialize(this.lang.raw("shop.team_upgrade")));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        // Back button
        if (slot == 49 && currentCategory != null) {
            openMain();
            return;
        }

        // Page navigation
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            render();
            return;
        }
        if (slot == 53) {
            List<Object> entries = new ArrayList<>();
            entries.addAll(currentCategory != null ? currentCategory.getChildren() : Collections.emptyList());
            entries.addAll(currentCategory != null ? currentCategory.getItems() : Collections.emptyList());
            int totalPages = (entries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
            if (currentPage < totalPages - 1) {
                currentPage++;
                render();
                return;
            }
        }

        if (currentCategory == null) {
            // Category selection
            if (slot < CATEGORY_SLOTS && slot < categories.size()) {
                ShopCategory cat = categories.get(slot);
                if (cat.isCategory() || !cat.getItems().isEmpty()) {
                    openCategory(cat);
                }
            }
        } else {
            // Item click or sub-category click
            List<Object> entries = new ArrayList<>();
            for (ShopCategory child : currentCategory.getChildren()) {
                entries.add(child);
            }
            entries.addAll(currentCategory.getItems());

            List<Integer> slots = computeSlots(entries, currentPage);
            int index = slots.indexOf(slot);
            if (index >= 0) {
                int globalIndex = currentPage * ITEMS_PER_PAGE + index;
                if (globalIndex < entries.size()) {
                    Object entry = entries.get(globalIndex);
                    if (entry instanceof ShopCategory cat) {
                        openCategory(cat);
                    } else if (entry instanceof ShopItem item) {
                        purchaseItem(item);
                    }
                }
            }
        }
    }

    private void purchaseItem(ShopItem item) {
        if (game == null) return;

        Material currencyMaterial = switch (item.getCurrency()) {
            case IRON -> Material.IRON_INGOT;
            case GOLD -> Material.GOLD_INGOT;
            case DIAMOND -> Material.DIAMOND;
            case EMERALD -> Material.EMERALD;
        };

        int price = item.getPrice();
        int has = countCurrency(player, currencyMaterial);

        if (has < price) {
            player.sendMessage(MM.deserialize(this.lang.raw("shop.not_enough", item.getCurrency().name().toLowerCase())));
            player.closeInventory();
            return;
        }

        removeCurrency(player, currencyMaterial, price);

        // Handle upgrades
        if (item.getUpgrade() != null) {
            handleUpgrade(item);
        }

        // Give items
        ItemStack bought = item.createItemStack();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(bought);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }

        // Fire event
        PlayerPurchaseEvent purchaseEvent = new PlayerPurchaseEvent(
                game,
                game.getGamePlayer(player),
                bought,
                price,
                item.getCurrency()
        );
        Bukkit.getPluginManager().callEvent(purchaseEvent);

        player.sendMessage(MM.deserialize(this.lang.raw("shop.purchased")));
    }

    private void handleUpgrade(ShopItem item) {
        switch (item.getUpgrade()) {
            case "forge" -> {
                var team = game.getPlayerTeam(player);
                if (team != null) {
                    var arenaTeam = game.getArena().getTeams().stream()
                            .filter(t -> t.getName().equals(team.getName()))
                            .findFirst().orElse(null);
                    if (arenaTeam != null) {
                        var forge = game.getArena().getGenerators().stream()
                                .filter(g -> g.getType().equalsIgnoreCase("forge"))
                                .filter(g -> team.getName().equalsIgnoreCase(g.getTeam()))
                                .findFirst().orElse(null);
                        if (forge != null && game instanceof dev.sebastianjnuwu.bedwars.game.Game g) {
                            g.upgradeForge(forge);
                        }
                    }
                }
            }
            default -> {
                // Other upgrades can be implemented later
            }
        }
    }

    private int countCurrency(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeCurrency(Player player, Material material, int amount) {
        int toRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int remove = Math.min(toRemove, item.getAmount());
                item.setAmount(item.getAmount() - remove);
                toRemove -= remove;
                if (item.getAmount() <= 0) {
                    player.getInventory().setItem(i, null);
                }
            }
        }
    }
}
