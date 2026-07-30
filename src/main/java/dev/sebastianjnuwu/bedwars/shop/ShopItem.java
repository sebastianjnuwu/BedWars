package dev.sebastianjnuwu.bedwars.shop;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;

public class ShopItem {

    private final Material material;
    private final int amount;
    private final String displayName;
    private final List<String> lore;
    private final Map<String, Integer> enchants;
    private final String tag;
    private final int price;
    private final CurrencyType currency;
    private final String upgrade;

    // Positioning
    private final int skip;
    private final Integer column;
    private final Integer row;
    private final String linebreak;
    private final String pagebreak;
    private final Integer absolute;

    private ShopItem(Builder builder) {
        this.material = builder.material;
        this.amount = builder.amount;
        this.displayName = builder.displayName;
        this.lore = builder.lore;
        this.enchants = builder.enchants;
        this.tag = builder.tag;
        this.price = builder.price;
        this.currency = builder.currency;
        this.upgrade = builder.upgrade;
        this.skip = builder.skip;
        this.column = builder.column;
        this.row = builder.row;
        this.linebreak = builder.linebreak;
        this.pagebreak = builder.pagebreak;
        this.absolute = builder.absolute;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public Map<String, Integer> getEnchants() {
        return enchants;
    }

    public String getTag() {
        return tag;
    }

    public int getPrice() {
        return price;
    }

    public CurrencyType getCurrency() {
        return currency;
    }

    public String getUpgrade() {
        return upgrade;
    }

    public int getSkip() {
        return skip;
    }

    public Integer getColumn() {
        return column;
    }

    public Integer getRow() {
        return row;
    }

    public String getLinebreak() {
        return linebreak;
    }

    public String getPagebreak() {
        return pagebreak;
    }

    public Integer getAbsolute() {
        return absolute;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createItemStack() {
        Material mat = material != null ? material : Material.BARRIER;
        ItemStack stack = new ItemStack(mat, amount);
        if (displayName != null || lore != null || (enchants != null && !enchants.isEmpty()) || tag != null) {
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
                    var key = org.bukkit.NamespacedKey.minecraft(entry.getKey().toLowerCase());
                    if (key != null) {
                        var enchant = org.bukkit.Registry.ENCHANTMENT.get(key);
                        if (enchant != null) {
                            meta.addEnchant(enchant, entry.getValue(), true);
                        }
                    }
                }
            }
            stack.setItemMeta(meta);
        }
        if (tag != null) {
            try {
                var tagObj = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(tag);
            } catch (Exception ignored) {
                try {
                    var compound = org.bukkit.Color.fromRGB(0);
                    stack = org.bukkit.inventory.ItemStack.of(stack.getType());
                } catch (Exception ignored) {}
            }
        }
        return stack;
    }

    public static class Builder {
        private Material material;
        private int amount = 1;
        private String displayName;
        private List<String> lore;
        private Map<String, Integer> enchants;
        private String tag;
        private int price;
        private CurrencyType currency;
        private String upgrade;
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

        public ShopItem build() {
            return new ShopItem(this);
        }
    }
}
