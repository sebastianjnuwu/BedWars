package dev.sebastianjnuwu.bedwars.shop.model;

import java.util.List;
import java.util.Map;

import org.bukkit.Material;

import dev.sebastianjnuwu.bedwars.api.model.CurrencyType;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;

/**
 * Construtor fluente de {@link ShopItem}.
 */
public class ShopItemBuilder {

    private Material material;
    private List<Material> armorSet;
    private List<ShopItem> contents;
    private int amount = 1;
    private String displayName;
    private List<String> lore;
    private Map<String, Integer> enchants;
    private String tag;
    private int price;
    private CurrencyType currency;
    private String upgrade;
    private UpgradeConfig upgradeConfig;
    private int skip;
    private Integer column;
    private Integer row;
    private String linebreak;
    private String pagebreak;
    private Integer absolute;

    Material getMaterial() {
        return material;
    }

    List<Material> getArmorSet() {
        return armorSet;
    }

    List<ShopItem> getContents() {
        return contents;
    }

    int getAmount() {
        return amount;
    }

    String getDisplayName() {
        return displayName;
    }

    List<String> getLore() {
        return lore;
    }

    Map<String, Integer> getEnchants() {
        return enchants;
    }

    String getTag() {
        return tag;
    }

    int getPrice() {
        return price;
    }

    CurrencyType getCurrency() {
        return currency;
    }

    String getUpgrade() {
        return upgrade;
    }

    UpgradeConfig getUpgradeConfig() {
        return upgradeConfig;
    }

    int getSkip() {
        return skip;
    }

    Integer getColumn() {
        return column;
    }

    Integer getRow() {
        return row;
    }

    String getLinebreak() {
        return linebreak;
    }

    String getPagebreak() {
        return pagebreak;
    }

    Integer getAbsolute() {
        return absolute;
    }

    public ShopItemBuilder material(Material material) {
        this.material = material;
        return this;
    }

    public ShopItemBuilder armorSet(List<Material> armorSet) {
        this.armorSet = armorSet;
        return this;
    }

    public ShopItemBuilder contents(List<ShopItem> contents) {
        this.contents = contents;
        return this;
    }

    public ShopItemBuilder amount(int amount) {
        this.amount = amount;
        return this;
    }

    public ShopItemBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ShopItemBuilder lore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ShopItemBuilder enchants(Map<String, Integer> enchants) {
        this.enchants = enchants;
        return this;
    }

    public ShopItemBuilder tag(String tag) {
        this.tag = tag;
        return this;
    }

    public ShopItemBuilder price(int price) {
        this.price = price;
        return this;
    }

    public ShopItemBuilder currency(CurrencyType currency) {
        this.currency = currency;
        return this;
    }

    public ShopItemBuilder upgrade(String upgrade) {
        this.upgrade = upgrade;
        return this;
    }

    public ShopItemBuilder upgradeConfig(UpgradeConfig upgradeConfig) {
        this.upgradeConfig = upgradeConfig;
        return this;
    }

    public ShopItemBuilder skip(int skip) {
        this.skip = skip;
        return this;
    }

    public ShopItemBuilder column(Integer column) {
        this.column = column;
        return this;
    }

    public ShopItemBuilder row(Integer row) {
        this.row = row;
        return this;
    }

    public ShopItemBuilder linebreak(String linebreak) {
        this.linebreak = linebreak;
        return this;
    }

    public ShopItemBuilder pagebreak(String pagebreak) {
        this.pagebreak = pagebreak;
        return this;
    }

    public ShopItemBuilder absolute(Integer absolute) {
        this.absolute = absolute;
        return this;
    }

    /**
     * Constrói o {@link ShopItem} final.
     *
     * @return item construído
     */
    public ShopItem build() {
        return new ShopItem(this);
    }
}
