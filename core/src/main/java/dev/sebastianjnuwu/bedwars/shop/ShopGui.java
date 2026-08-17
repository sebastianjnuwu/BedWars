package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.game.GameItems;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * GUI da loja de BedWars.
 * <p>
 * Mantém o estado da loja aberta (categorias, página, título) e delega a
 * renderização e a compra de itens aos helpers {@link ShopGuiRenderer} e
 * {@link ShopPurchase}.
 * </p>
 */
public class ShopGui implements InventoryHolder {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int CATEGORY_SLOTS = 9;
    private static final int ITEMS_START = 9;
    private static final int ITEMS_END = 44;
    private static final int ITEMS_PER_PAGE = 36;

    private static final Map<UUID, ShopGui> openGuis = new HashMap<>();

    final LangManager lang;
    final Player player;
    final Game game;
    final List<ShopCategory> categories;
    final String baseTitle;

    Inventory inventory;
    ShopCategory currentCategory;
    int currentPage;
    Component currentTitleComponent;

    private final ShopGuiRenderer renderer;
    private final ShopPurchase purchase;

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

        this.renderer = new ShopGuiRenderer(this);
        this.purchase = new ShopPurchase(this);

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
        this.renderer.render();
    }

    /**
     * Abre uma categoria (ou subcategoria) específica.
     *
     * @param category categoria a abrir
     */
    public void openCategory(ShopCategory category) {
        this.currentCategory = category;
        this.currentPage = 0;
        this.renderer.render();
    }

    void render() {
        this.renderer.render();
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
            this.renderer.render();
            return;
        }
        if (slot == 53) {
            List<Object> entries = new ArrayList<>();
            entries.addAll(currentCategory != null ? currentCategory.getChildren() : Collections.emptyList());
            entries.addAll(currentCategory != null ? currentCategory.getItems() : Collections.emptyList());
            int totalPages = (entries.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
            if (currentPage < totalPages - 1) {
                currentPage++;
                this.renderer.render();
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

        List<Integer> slots = this.renderer.computeSlots(entries, currentPage);
        int index = slots.indexOf(slot);
        if (index >= 0) {
            int globalIndex = currentPage * ITEMS_PER_PAGE + index;
            if (globalIndex < entries.size()) {
                Object entry = entries.get(globalIndex);
                if (entry instanceof ShopCategory cat) {
                    openCategory(cat);
                } else if (entry instanceof ShopItem item) {
                    this.purchase.purchaseItem(item);
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    void applyTeamColor(final ItemStack stack) {
        if (stack == null || this.game == null || stack.getType() != Material.WHITE_WOOL) {
            return;
        }
        final ArenaTeam team = this.game.getPlayerTeam(this.player);
        if (team == null) {
            return;
        }
        stack.setType(GameItems.getWoolColor(team.getColor()));
    }

    @Nullable ForgeLevel teamUpgradeLevel(final String upgrade) {
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

    String upgradeName(final String upgrade) {
        return switch (upgrade) {
            case "sharpness" -> "Afiação";
            case "protection" -> "Proteção";
            default -> upgrade;
        };
    }

    @Nullable ForgeLevel forgeUpgradeLevel() {
        final ArenaGenerator forge = findPlayerForge();
        if (forge == null) {
            return null;
        }
        return this.game.getForgeUpgradeLevel(forge);
    }

    @Nullable ArenaGenerator findPlayerForge() {
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

    String currencyName(final Material material) {
        return switch (material) {
            case IRON_INGOT -> this.lang.raw("shop.currency_iron");
            case GOLD_INGOT -> this.lang.raw("shop.currency_gold");
            case DIAMOND -> this.lang.raw("shop.currency_diamond");
            case EMERALD -> this.lang.raw("shop.currency_emerald");
            default -> this.lang.raw("shop.currency_diamond");
        };
    }

    String currencyName(final CurrencyType currency) {
        return switch (currency) {
            case IRON -> this.lang.raw("shop.currency_iron");
            case GOLD -> this.lang.raw("shop.currency_gold");
            case DIAMOND -> this.lang.raw("shop.currency_diamond");
            case EMERALD -> this.lang.raw("shop.currency_emerald");
        };
    }
}