package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.events.PlayerPurchaseEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * GUI da loja de BedWars.
 * <p>
 * Controla a renderização das categorias e itens, paginação, posicionamento
 * (linhas/colunas/centralização), compra de itens, kits recursivos, armaduras
 * de time e upgrades.
 * </p>
 */
public class ShopGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CATEGORY_SLOTS = 9;
    private static final int ITEMS_START = 9;
    private static final int ITEMS_END = 44;
    private static final int ITEMS_PER_PAGE = 36;

    private final LangManager lang;
    private final Player player;
    private final Game game;

    private Inventory inventory;
    private final List<ShopCategory> categories;
    private ShopCategory currentCategory;
    private int currentPage;
    private final String baseTitle;
    private Component currentTitleComponent;

    private static final Map<UUID, ShopGui> openGuis = new HashMap<>();

    /**
     * Cria e abre a loja para o jogador.
     *
     * @param shopManager gerenciador de lojas
     * @param lang        gerenciador de idiomas
     * @param player      jogador que abriu a loja
     * @param game        partida em que o jogador está
     * @param shopName    nome da loja a abrir
     */
    public ShopGui(ShopManager shopManager, LangManager lang, Player player, Game game, String shopName) {
        this.lang = lang;
        this.player = player;
        this.game = game;
        this.categories = shopManager.getCategories(shopName);
        this.currentPage = 0;

        final String displayName = shopManager.getDisplayName(shopName);
        this.baseTitle = displayName != null ? displayName : this.lang.raw("shop.title");
        this.inventory = Bukkit.createInventory(this, 54, MM.deserialize(baseTitle));
        this.currentTitleComponent = MM.deserialize(baseTitle);

        openGuis.put(player.getUniqueId(), this);
        openMain();
        player.openInventory(inventory);
    }

    /**
     * Retorna a loja aberta de um jogador, se houver.
     *
     * @param player jogador
     * @return loja aberta ou {@code null}
     */
    public static ShopGui getOpenGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    /**
     * Remove o registro da loja aberta de um jogador.
     *
     * @param player jogador
     */
    public static void removeOpenGui(Player player) {
        openGuis.remove(player.getUniqueId());
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Abre a lista principal de categorias.
     */
    public void openMain() {
        this.currentCategory = null;
        this.currentPage = 0;
        render();
    }

    /**
     * Abre uma categoria (ou subcategoria) específica.
     *
     * @param category categoria a abrir
     */
    public void openCategory(ShopCategory category) {
        this.currentCategory = category;
        this.currentPage = 0;
        render();
    }

    private void render() {
        final Component title = buildTitle();
        if (!title.equals(this.currentTitleComponent)) {
            this.inventory = Bukkit.createInventory(this, 54, title);
            this.currentTitleComponent = title;
            this.player.openInventory(this.inventory);
        }

        inventory.clear();

        fillBorder();

        if (currentCategory == null) {
            renderCategoryList();
        } else {
            renderCategoryItems();
        }
    }

    private Component buildTitle() {
        if (currentCategory == null) {
            return MM.deserialize(this.baseTitle);
        }
        final String categoryName = this.currentCategory.getDisplayName() != null
                ? this.currentCategory.getDisplayName()
                : this.currentCategory.getName();
        return MM.deserialize(this.baseTitle + " > " + categoryName);
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
            if (slot >= CATEGORY_SLOTS) {
                break;
            }
            ItemStack icon = cat.createIconItem();
            inventory.setItem(slot, icon);
            slot++;
        }
    }

    /**
     * Renderiza os produtos da categoria ativa.
     * <p>
     * Diferente da visão principal, aqui a fileira do topo de categorias é omitida:
     * a linha já fica coberta pela borda e aparecem apenas os produtos, com botões de
     * navegação de página (slots 45/53) e um botão "Voltar às categorias" (slot 49)
     * para retornar à lista principal.
     * </p>
     */
    private void renderCategoryItems() {
        List<Object> entries = new ArrayList<>();
        for (ShopCategory child : currentCategory.getChildren()) {
            entries.add(child);
        }
        entries.addAll(currentCategory.getItems());

        List<Integer> slots = computeSlots(entries, currentPage);

        int itemIndex = 0;
        for (int i = 0; i < entries.size(); i++) {
            int page = i / ITEMS_PER_PAGE;
            if (page != currentPage) {
                continue;
            }

            Object entry = entries.get(i);
            if (entry instanceof ShopCategory cat) {
                int slot = slots.get(itemIndex++);
                if (slot >= ITEMS_START) {
                    inventory.setItem(slot, cat.createIconItem());
                }
            } else if (entry instanceof ShopItem item) {
                int slot = slots.get(itemIndex++);
                if (slot >= ITEMS_START) {
                    ItemStack displayStack = createDisplayItem(item);
                    inventory.setItem(slot, displayStack);
                }
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
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(MM.deserialize(this.lang.raw("shop.back_categories")));
        back.setItemMeta(backMeta);
        inventory.setItem(49, back);
    }

    private List<Integer> computeSlots(List<Object> entries, int page) {
        List<Integer> slots = new ArrayList<>();
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());
        int count = endIndex - startIndex;

        final boolean vertical = currentCategory != null && "column".equals(currentCategory.getLayoutType());
        final boolean center = currentCategory != null && currentCategory.isCentered();

        int[] gridRows = new int[count];
        int[] gridCols = new int[count];
        boolean[] placed = new boolean[count];
        boolean[] absolute = new boolean[count];

        int row = 0;
        int col = 0;

        for (int i = startIndex; i < endIndex; i++) {
            int idx = i - startIndex;
            Object entry = entries.get(i);

            if (entry instanceof ShopItem item) {
                if (item.getAbsolute() != null) {
                    int slot = item.getAbsolute();
                    if (slot >= ITEMS_START && slot <= ITEMS_END) {
                        gridRows[idx] = (slot - ITEMS_START) / 9;
                        gridCols[idx] = (slot - ITEMS_START) % 9;
                        placed[idx] = true;
                        absolute[idx] = true;
                    }
                    continue;
                }
                if (item.getLinebreak() != null) {
                    switch (item.getLinebreak().toLowerCase()) {
                        case "before" -> {
                            row++;
                            col = 0;
                        }
                        case "after" -> {
                            placeItem(gridRows, gridCols, placed, idx, row, col);
                            row++;
                            col = 0;
                            continue;
                        }
                        case "both" -> {
                            row++;
                            col = 0;
                            placeItem(gridRows, gridCols, placed, idx, row, col);
                            row++;
                            col = 0;
                            continue;
                        }
                        default -> { }
                    }
                }
                if (item.getPagebreak() != null) {
                    continue;
                }
                if (!vertical) {
                    if (item.getColumn() != null) {
                        col = item.getColumn();
                    }
                    if (item.getRow() != null) {
                        row = item.getRow() - 1;
                        col = 0;
                    }
                }
                if (item.getSkip() > 0) {
                    if (vertical) {
                        row += item.getSkip();
                    } else {
                        col += item.getSkip();
                    }
                }
            }

            if (!vertical) {
                if (col > 8) {
                    row++;
                    col = 0;
                }
                if (row <= 3) {
                    placeItem(gridRows, gridCols, placed, idx, row, col);
                    col++;
                    if (col >= 9) {
                        row++;
                        col = 0;
                    }
                }
            } else {
                if (row > 3) {
                    row = 0;
                    col++;
                }
                if (col <= 8) {
                    placeItem(gridRows, gridCols, placed, idx, row, col);
                    row++;
                    if (row > 3) {
                        row = 0;
                        col++;
                    }
                }
            }
        }

        if (center) {
            centerGrid(gridRows, gridCols, placed, absolute, vertical);
        }

        for (int i = 0; i < count; i++) {
            if (placed[i]) {
                slots.add(ITEMS_START + gridRows[i] * 9 + gridCols[i]);
            } else {
                slots.add(-1);
            }
        }
        return slots;
    }

    private static void placeItem(int[] gridRows, int[] gridCols, boolean[] placed,
            int idx, int row, int col) {
        gridRows[idx] = row;
        gridCols[idx] = col;
        placed[idx] = true;
    }

    private static void centerGrid(int[] gridRows, int[] gridCols, boolean[] placed,
            boolean[] absolute, boolean vertical) {
        if (vertical) {
            for (int c = 0; c < 9; c++) {
                int minRow = Integer.MAX_VALUE;
                int maxRow = Integer.MIN_VALUE;
                boolean hasAbsolute = false;
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridCols[i] == c) {
                        hasAbsolute |= absolute[i];
                        minRow = Math.min(minRow, gridRows[i]);
                        maxRow = Math.max(maxRow, gridRows[i]);
                    }
                }
                if (maxRow == Integer.MIN_VALUE || hasAbsolute) {
                    continue;
                }
                int offset = (4 - (maxRow - minRow + 1)) / 2 - minRow;
                if (offset == 0) {
                    continue;
                }
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridCols[i] == c) {
                        gridRows[i] += offset;
                    }
                }
            }
        } else {
            for (int r = 0; r < 4; r++) {
                int minCol = Integer.MAX_VALUE;
                int maxCol = Integer.MIN_VALUE;
                boolean hasAbsolute = false;
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridRows[i] == r) {
                        hasAbsolute |= absolute[i];
                        minCol = Math.min(minCol, gridCols[i]);
                        maxCol = Math.max(maxCol, gridCols[i]);
                    }
                }
                if (maxCol == Integer.MIN_VALUE || hasAbsolute) {
                    continue;
                }
                int offset = (9 - (maxCol - minCol + 1)) / 2 - minCol;
                if (offset == 0) {
                    continue;
                }
                for (int i = 0; i < placed.length; i++) {
                    if (placed[i] && gridRows[i] == r) {
                        gridCols[i] += offset;
                    }
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void applyTeamColor(final ItemStack stack) {
        if (stack == null || this.game == null || stack.getType() != Material.WHITE_WOOL) {
            return;
        }
        final ArenaTeam team = this.game.getPlayerTeam(this.player);
        if (team == null) {
            return;
        }
        stack.setType(Game.getWoolColor(team.getColor()));
    }

    private ItemStack createDisplayItem(ShopItem item) {
        ItemStack stack = item.createItemStack();
        applyTeamColor(stack);
        ItemMeta meta = stack.getItemMeta();

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());

        if ("forge".equals(item.getUpgrade())) {
            final ForgeLevel next = forgeUpgradeLevel();
            if (next == null || next.upgradeMaterial() == null) {
                stack = new ItemStack(Material.RED_STAINED_GLASS_PANE, stack.getAmount());
                meta = stack.getItemMeta();
                lore.add(MM.deserialize(this.lang.raw("shop.forge_maxed")));
            } else {
                lore.add(MM.deserialize(this.lang.raw("shop.price", String.valueOf(next.upgradePrice()), this.currencyName(next.upgradeMaterial()))));
                lore.add(MM.deserialize(this.lang.raw("shop.forge_next_level", String.valueOf(next.level()))));
            }
        } else if (item.getUpgrade() != null) {
            final ForgeLevel next = teamUpgradeLevel(item.getUpgrade());
            if (next == null || next.upgradeMaterial() == null) {
                stack = new ItemStack(Material.RED_STAINED_GLASS_PANE, stack.getAmount());
                meta = stack.getItemMeta();
                lore.add(MM.deserialize(this.lang.raw("shop.upgrade_maxed", upgradeName(item.getUpgrade()))));
            } else {
                lore.add(MM.deserialize(this.lang.raw("shop.price", String.valueOf(next.upgradePrice()), this.currencyName(next.upgradeMaterial()))));
                lore.add(MM.deserialize(this.lang.raw("shop.forge_next_level", String.valueOf(next.level()))));
            }
        } else {
            String currencyName = switch (item.getCurrency()) {
                case IRON -> this.lang.raw("shop.currency_iron");
                case GOLD -> this.lang.raw("shop.currency_gold");
                case DIAMOND -> this.lang.raw("shop.currency_diamond");
                case EMERALD -> this.lang.raw("shop.currency_emerald");
            };
            lore.add(MM.deserialize(this.lang.raw("shop.price", String.valueOf(item.getPrice()), currencyName)));
        }

        if (item.getUpgrade() != null) {
            lore.add(MM.deserialize(this.lang.raw("shop.team_upgrade")));
        }

        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Processa um clique na GUI da loja.
     * <p>
     * Trata botões de voltar, navegação de página, abertura de categorias e
     * compra de itens.
     * </p>
     *
     * @param event evento de clique
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) {
            return;
        }

        event.setCancelled(true);

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

        // Category navigation in the top row (main view and inside a category)
        if (slot < CATEGORY_SLOTS && slot < categories.size()) {
            ShopCategory cat = categories.get(slot);
            if (cat.isCategory() || !cat.getItems().isEmpty()) {
                openCategory(cat);
            }
            return;
        }

        if (currentCategory == null) {
            return;
        }

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

    /**
     * Executa a compra de um item da loja.
     * <p>
     * Valida o saldo do jogador, bloqueia recompra de armadura já equipada
     * (mesma ou melhor) e entrega o item (upgrade, kit recursivo, conjunto de
     * armadura ou item simples), disparando {@link PlayerPurchaseEvent}.
     * </p>
     *
     * @param item item a ser comprado
     */
    private void purchaseItem(ShopItem item) {
        if (game == null) {
            return;
        }

        final Material currencyMaterial;
        final int price;
        final String upgrade = item.getUpgrade();
        if ("forge".equals(upgrade)) {
            final ForgeLevel next = forgeUpgradeLevel();
            if (next == null || next.upgradeMaterial() == null) {
                CompatProvider.chat().sendMessage(player, MM.deserialize(this.lang.raw("shop.forge_maxed")));
                player.closeInventory();
                return;
            }
            price = next.upgradePrice();
            currencyMaterial = next.upgradeMaterial();
        } else if (upgrade != null) {
            final ForgeLevel next = teamUpgradeLevel(upgrade);
            if (next == null || next.upgradeMaterial() == null) {
                CompatProvider.chat().sendMessage(player, MM.deserialize(this.lang.raw("shop.upgrade_maxed", upgradeName(upgrade))));
                player.closeInventory();
                return;
            }
            price = next.upgradePrice();
            currencyMaterial = next.upgradeMaterial();
        } else {
            currencyMaterial = switch (item.getCurrency()) {
                case IRON -> Material.IRON_INGOT;
                case GOLD -> Material.GOLD_INGOT;
                case DIAMOND -> Material.DIAMOND;
                case EMERALD -> Material.EMERALD;
            };
            price = item.getPrice();
        }

        if (alreadyHasArmor(item)) {
            CompatProvider.chat().sendMessage(player, MM.deserialize(this.lang.raw("shop.armor_already_owned")));
            player.closeInventory();
            return;
        }

        int has = countCurrency(player, currencyMaterial);

        if (has < price) {
            CompatProvider.chat().sendMessage(player, MM.deserialize(this.lang.raw("shop.not_enough", currencyMaterial.name().toLowerCase())));
            player.closeInventory();
            return;
        }

        removeCurrency(player, currencyMaterial, price);

        // Handle upgrades
        ItemStack bought = item.createItemStack();
        if (item.getUpgrade() != null) {
            handleUpgrade(item);
        } else if (item.hasContents()) {
            giveContents(item);
        } else if (item.getArmorSet() != null) {
            for (ItemStack piece : item.createArmorSetItems()) {
                deliverItem(piece);
            }
        } else {
            deliverItem(bought);
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

        CompatProvider.chat().sendMessage(player, MM.deserialize(this.lang.raw("shop.purchased")));

        this.render();
    }

    private void giveContents(ShopItem item) {
        for (ShopItem child : item.getContents()) {
            if (child.hasContents()) {
                giveContents(child);
            } else if (child.getArmorSet() != null) {
                for (ItemStack piece : child.createArmorSetItems()) {
                    deliverItem(piece);
                }
            } else {
                deliverItem(child.createItemStack());
            }
        }
    }

    private boolean alreadyHasArmor(final ShopItem item) {
        if (item.getUpgrade() != null) {
            return false;
        }
        final List<ItemStack> pieces = new ArrayList<>();
        collectArmorPieces(item, pieces);
        if (pieces.isEmpty()) {
            return false;
        }
        for (final ItemStack piece : pieces) {
            final ItemStack equipped = equippedArmor(piece.getType());
            if (effectivePoints(piece) > effectivePoints(equipped)) {
                return false;
            }
        }
        return true;
    }

    private void collectArmorPieces(final ShopItem item, final List<ItemStack> pieces) {
        if (item.hasContents()) {
            for (final ShopItem child : item.getContents()) {
                collectArmorPieces(child, pieces);
            }
        } else if (item.getArmorSet() != null) {
            pieces.addAll(item.createArmorSetItems());
        } else if (item.getMaterial() != null && leatherFor(item.getMaterial()) != null) {
            pieces.add(item.createItemStack());
        }
    }

    private ItemStack equippedArmor(final Material material) {
        final Material leather = leatherFor(material);
        if (leather == null) {
            return null;
        }
        return switch (leather) {
            case LEATHER_HELMET -> this.player.getInventory().getHelmet();
            case LEATHER_CHESTPLATE -> this.player.getInventory().getChestplate();
            case LEATHER_LEGGINGS -> this.player.getInventory().getLeggings();
            case LEATHER_BOOTS -> this.player.getInventory().getBoots();
            default -> null;
        };
    }

    private static int effectivePoints(final ItemStack stack) {
        if (stack == null) {
            return 0;
        }
        int points = armorPoints(stack.getType());
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            final var modifiers = meta.getAttributeModifiers(Attribute.ARMOR);
            if (modifiers != null) {
                for (final AttributeModifier modifier : modifiers) {
                    if (modifier.getKey().equals(NamespacedKey.minecraft("bw_armor"))) {
                        points += (int) modifier.getAmount();
                    }
                }
            }
        }
        return points;
    }

    private void deliverItem(final ItemStack stack) {
        if (leatherFor(stack.getType()) != null) {
            equipTeamArmor(stack);
        } else {
            applyTeamColor(stack);
            applyTeamSharpness(stack);
            giveOrDrop(stack);
        }
    }

    private void applyTeamSharpness(final ItemStack stack) {
        if (stack.getType() == Material.AIR || !stack.getType().name().endsWith("_SWORD")) {
            return;
        }
        final ArenaTeam team = this.game.getPlayerTeam(this.player);
        final int level = this.game.getSharpnessLevel(team);
        if (level > 0) {
            stack.addUnsafeEnchantment(Enchantment.SHARPNESS, level);
        }
    }

    private void equipTeamArmor(final ItemStack stack) {
        final Material leather = leatherFor(stack.getType());
        if (leather == null) {
            applyTeamColor(stack);
            giveOrDrop(stack);
            return;
        }
        final ArenaTeam team = this.game.getPlayerTeam(this.player);
        final Color color = team != null ? Game.getArmorColor(team.getColor()) : Color.WHITE;
        final ItemStack colored = new ItemStack(leather);
        final ItemMeta sourceMeta = stack.getItemMeta();
        final LeatherArmorMeta meta = (LeatherArmorMeta) colored.getItemMeta();
        if (sourceMeta != null) {
            meta.displayName(sourceMeta.displayName());
            if (sourceMeta.hasLore()) {
                meta.lore(sourceMeta.lore());
            }
            sourceMeta.getEnchants().forEach((enchant, level) -> meta.addEnchant(enchant, level, true));
        }
        meta.setColor(color);
        meta.setUnbreakable(true);
        final int delta = armorPoints(stack.getType()) - armorPoints(leather);
        if (delta > 0) {
            meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(
                    NamespacedKey.minecraft("bw_armor"),
                    delta,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        final int toughness = armorToughness(stack.getType());
        if (toughness > 0) {
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                    NamespacedKey.minecraft("bw_toughness"),
                    toughness,
                    AttributeModifier.Operation.ADD_NUMBER));
        }
        colored.setItemMeta(meta);
        applyTeamProtection(colored, team);
        switch (leather) {
            case LEATHER_HELMET -> this.player.getInventory().setHelmet(colored);
            case LEATHER_CHESTPLATE -> this.player.getInventory().setChestplate(colored);
            case LEATHER_LEGGINGS -> this.player.getInventory().setLeggings(colored);
            case LEATHER_BOOTS -> this.player.getInventory().setBoots(colored);
            default -> { }
        }
    }

    private void applyTeamProtection(final ItemStack stack, final ArenaTeam team) {
        final int level = this.game.getProtectionLevel(team);
        if (level > 0) {
            stack.addUnsafeEnchantment(Enchantment.PROTECTION, level);
        }
    }

    private static Material leatherFor(final Material mat) {
        final String name = mat.name();
        if (name.endsWith("_HELMET")) {
            return Material.LEATHER_HELMET;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return Material.LEATHER_CHESTPLATE;
        }
        if (name.endsWith("_LEGGINGS")) {
            return Material.LEATHER_LEGGINGS;
        }
        if (name.endsWith("_BOOTS")) {
            return Material.LEATHER_BOOTS;
        }
        return null;
    }

    private static int armorPoints(final Material mat) {
        return switch (mat) {
            case LEATHER_HELMET, LEATHER_BOOTS, GOLDEN_BOOTS, CHAINMAIL_BOOTS -> 1;
            case GOLDEN_HELMET, CHAINMAIL_HELMET, IRON_HELMET, LEATHER_LEGGINGS, IRON_BOOTS -> 2;
            case LEATHER_CHESTPLATE, DIAMOND_HELMET, NETHERITE_HELMET, GOLDEN_LEGGINGS,
                    CHAINMAIL_LEGGINGS, DIAMOND_BOOTS, NETHERITE_BOOTS -> 3;
            case GOLDEN_CHESTPLATE, CHAINMAIL_CHESTPLATE, IRON_LEGGINGS -> 5;
            case IRON_CHESTPLATE, DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> 6;
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 8;
            default -> 0;
        };
    }

    private static int armorToughness(final Material mat) {
        return switch (mat) {
            case DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 2;
            case NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 3;
            default -> 0;
        };
    }

    private void giveOrDrop(ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
            }
        }
    }

    private @Nullable ForgeLevel teamUpgradeLevel(final String upgrade) {
        final ArenaTeam team = game.getPlayerTeam(player);
        if (team == null) {
            return null;
        }
        return switch (upgrade) {
            case "sharpness" -> game.getSharpnessUpgradeLevel(team);
            case "protection" -> game.getProtectionUpgradeLevel(team);
            default -> null;
        };
    }

    private String upgradeName(final String upgrade) {
        return switch (upgrade) {
            case "sharpness" -> "Afiação";
            case "protection" -> "Proteção";
            default -> upgrade;
        };
    }

    private void handleUpgrade(ShopItem item) {
        final ArenaTeam team = game.getPlayerTeam(player);
        if (team == null) {
            return;
        }
        switch (item.getUpgrade()) {
            case "forge" -> {
                final ArenaGenerator forge = game.getArena().getGenerators().stream()
                        .filter(g -> g.getType().equalsIgnoreCase("forge"))
                        .filter(g -> team.getName().equalsIgnoreCase(g.getTeam()))
                        .findFirst().orElse(null);
                if (forge != null) {
                    game.upgradeForge(forge);
                }
            }
            case "sharpness" -> game.upgradeSharpness(team);
            case "protection" -> game.upgradeProtection(team);
            default -> {
                // Outros upgrades não implementados
            }
        }
    }

    private @Nullable ForgeLevel forgeUpgradeLevel() {
        final ArenaGenerator forge = findPlayerForge();
        if (forge == null) {
            return null;
        }
        return this.game.getForgeUpgradeLevel(forge);
    }

    private @Nullable ArenaGenerator findPlayerForge() {
        final ArenaTeam team = this.game.getPlayerTeam(this.player);
        if (team == null) {
            return null;
        }
        for (final ArenaGenerator gen : this.game.getArena().getGenerators()) {
            if (gen.getType().equalsIgnoreCase("forge") && team.getName().equalsIgnoreCase(gen.getTeam())) {
                return gen;
            }
        }
        return null;
    }

    private String currencyName(final Material material) {
        return switch (material) {
            case IRON_INGOT -> this.lang.raw("shop.currency_iron");
            case GOLD_INGOT -> this.lang.raw("shop.currency_gold");
            case DIAMOND -> this.lang.raw("shop.currency_diamond");
            case EMERALD -> this.lang.raw("shop.currency_emerald");
            default -> this.lang.raw("shop.currency_diamond");
        };
    }

    private String currencyName(final CurrencyType currency) {
        return switch (currency) {
            case IRON -> this.lang.raw("shop.currency_iron");
            case GOLD -> this.lang.raw("shop.currency_gold");
            case DIAMOND -> this.lang.raw("shop.currency_diamond");
            case EMERALD -> this.lang.raw("shop.currency_emerald");
        };
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
