package dev.sebastianjnuwu.bedwars.shop;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.events.PlayerPurchaseEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;

/**
 * Responsável pela compra de itens na loja de BedWars.
 * <p>
 * Valida o saldo, bloqueia recompra de armadura já equipada, desconta a
 * moeda e entrega o item comprado (upgrade, kit recursivo, conjunto de
 * armadura ou item simples), disparando {@link PlayerPurchaseEvent}.
 * </p>
 */
class ShopPurchase {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ShopGui gui;
    private final ShopArmorLogic armorLogic;

    ShopPurchase(final ShopGui gui) {
        this.gui = gui;
        this.armorLogic = new ShopArmorLogic(gui);
    }

    void purchaseItem(ShopItem item) {
        if (gui.game == null) {
            return;
        }

        final Material currencyMaterial;
        final int price;
        final String upgrade = item.getUpgrade();
        if ("forge".equals(upgrade)) {
            final ForgeLevel next = gui.forgeUpgradeLevel();
            if (next == null || next.upgradeMaterial() == null) {
                CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.forge_maxed")));
                gui.player.closeInventory();
                return;
            }
            price = next.upgradePrice();
            currencyMaterial = next.upgradeMaterial();
        } else if (upgrade != null) {
            final ForgeLevel next = gui.teamUpgradeLevel(upgrade);
            if (next == null || next.upgradeMaterial() == null) {
                CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.upgrade_maxed", gui.upgradeName(upgrade))));
                gui.player.closeInventory();
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

        if (this.armorLogic.alreadyHasArmor(item)) {
            CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.armor_already_owned")));
            gui.player.closeInventory();
            return;
        }

        int has = countCurrency(gui.player, currencyMaterial);

        if (has < price) {
            CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.not_enough", gui.currencyName(currencyMaterial))));
            gui.player.closeInventory();
            return;
        }

        removeCurrency(gui.player, currencyMaterial, price);

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

        PlayerPurchaseEvent purchaseEvent = new PlayerPurchaseEvent(
                gui.game,
                gui.game.getGamePlayer(gui.player),
                bought,
                price,
                item.getCurrency()
        );
        Bukkit.getPluginManager().callEvent(purchaseEvent);

        CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.purchased")));

        gui.render();
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

    private void deliverItem(final ItemStack stack) {
        if (ShopArmorLogic.leatherFor(stack.getType()) != null) {
            this.armorLogic.equipTeamArmor(stack);
        } else {
            gui.applyTeamColor(stack);
            applyTeamSharpness(stack);
            giveOrDrop(stack);
        }
    }

    private void applyTeamSharpness(final ItemStack stack) {
        if (stack.getType() == Material.AIR || !stack.getType().name().endsWith("_SWORD")) {
            return;
        }
        final ArenaTeam team = gui.game.getPlayerTeam(gui.player);
        final int level = gui.game.getSharpnessLevel(team);
        if (level > 0) {
            stack.addUnsafeEnchantment(Enchantment.SHARPNESS, level);
        }
    }

    private void giveOrDrop(ItemStack stack) {
        Map<Integer, ItemStack> leftover = gui.player.getInventory().addItem(stack);
        if (!leftover.isEmpty()) {
            for (ItemStack drop : leftover.values()) {
                gui.player.getWorld().dropItem(gui.player.getLocation(), drop);
            }
        }
    }

    private void handleUpgrade(ShopItem item) {
        final ArenaTeam team = gui.game.getPlayerTeam(gui.player);
        if (team == null) {
            return;
        }
        switch (item.getUpgrade()) {
            case "forge" -> {
                final ArenaGenerator forge = gui.game.getArena().getGenerators().stream()
                        .filter(g -> g.getType().equalsIgnoreCase("forge"))
                        .filter(g -> team.getName().equalsIgnoreCase(g.getTeam()))
                        .findFirst().orElse(null);
                if (forge != null) {
                    gui.game.upgradeForge(forge);
                }
            }
            case "sharpness" -> gui.game.upgradeSharpness(team);
            case "protection" -> gui.game.upgradeProtection(team);
            default -> {
                // Outros upgrades não implementados
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