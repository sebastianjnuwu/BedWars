package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.events.PlayerPurchaseEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.GameItems;

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

    ShopPurchase(final ShopGui gui) {
        this.gui = gui;
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

        if (alreadyHasArmor(item)) {
            CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.armor_already_owned")));
            gui.player.closeInventory();
            return;
        }

        int has = countCurrency(gui.player, currencyMaterial);

        if (has < price) {
            CompatProvider.chat().sendMessage(gui.player, MM.deserialize(gui.lang.raw("shop.not_enough", currencyMaterial.name().toLowerCase())));
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
            case LEATHER_HELMET -> gui.player.getInventory().getHelmet();
            case LEATHER_CHESTPLATE -> gui.player.getInventory().getChestplate();
            case LEATHER_LEGGINGS -> gui.player.getInventory().getLeggings();
            case LEATHER_BOOTS -> gui.player.getInventory().getBoots();
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

    private void equipTeamArmor(final ItemStack stack) {
        final Material leather = leatherFor(stack.getType());
        if (leather == null) {
            gui.applyTeamColor(stack);
            giveOrDrop(stack);
            return;
        }
        final ArenaTeam team = gui.game.getPlayerTeam(gui.player);
        final Color color = team != null ? GameItems.getArmorColor(team.getColor()) : Color.WHITE;
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
            case LEATHER_HELMET -> gui.player.getInventory().setHelmet(colored);
            case LEATHER_CHESTPLATE -> gui.player.getInventory().setChestplate(colored);
            case LEATHER_LEGGINGS -> gui.player.getInventory().setLeggings(colored);
            case LEATHER_BOOTS -> gui.player.getInventory().setBoots(colored);
            default -> { }
        }
    }

    private void applyTeamProtection(final ItemStack stack, final ArenaTeam team) {
        final int level = gui.game.getProtectionLevel(team);
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