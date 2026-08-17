package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.game.GameItems;

/**
 * Lógica de armaduras da loja: verificação de recompra de armadura já
 * equipada, cálculo de pontos efetivos e conversão para armadura de couro
 * tingida na cor do time com atributos preservados.
 */
final class ShopArmorLogic {

    private final ShopGui gui;

    ShopArmorLogic(final ShopGui gui) {
        this.gui = gui;
    }

    boolean alreadyHasArmor(final ShopItem item) {
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

    void equipTeamArmor(final ItemStack stack) {
        final Material leather = leatherFor(stack.getType());
        if (leather == null) {
            this.gui.applyTeamColor(stack);
            return;
        }
        final ArenaTeam team = this.gui.game.getPlayerTeam(this.gui.player);
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
        final int level = this.gui.game.getProtectionLevel(team);
        if (level > 0) {
            colored.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION, level);
        }
        switch (leather) {
            case LEATHER_HELMET -> this.gui.player.getInventory().setHelmet(colored);
            case LEATHER_CHESTPLATE -> this.gui.player.getInventory().setChestplate(colored);
            case LEATHER_LEGGINGS -> this.gui.player.getInventory().setLeggings(colored);
            case LEATHER_BOOTS -> this.gui.player.getInventory().setBoots(colored);
            default -> { }
        }
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
            case LEATHER_HELMET -> this.gui.player.getInventory().getHelmet();
            case LEATHER_CHESTPLATE -> this.gui.player.getInventory().getChestplate();
            case LEATHER_LEGGINGS -> this.gui.player.getInventory().getLeggings();
            case LEATHER_BOOTS -> this.gui.player.getInventory().getBoots();
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

    static Material leatherFor(final Material mat) {
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
}