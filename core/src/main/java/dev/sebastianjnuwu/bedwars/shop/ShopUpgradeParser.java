package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;

/**
 * Converte a subseção {@code upgrade-config:} dos itens da loja em objetos
 * {@link UpgradeConfig} e {@link ForgeLevel}.
 */
final class ShopUpgradeParser {

    private ShopUpgradeParser() {
    }

    static UpgradeConfig parseUpgradeConfig(Map<?, ?> map) {
        int maxLevel = parsePositiveInt(map.get("max-level"), 1);
        int levelDefault = parsePositiveInt(map.get("level-default"), 0);
        List<ForgeLevel> levels = new ArrayList<>();
        Object levelsObj = map.get("levels");
        if (levelsObj instanceof Map) {
            for (var levelEntry : ((Map<?, ?>) levelsObj).entrySet()) {
                ForgeLevel level = parseUpgradeLevel(levelEntry.getKey(), levelEntry.getValue());
                if (level != null) {
                    levels.add(level);
                }
            }
        }
        return new UpgradeConfig(maxLevel, levelDefault, levels);
    }

    private static @Nullable ForgeLevel parseUpgradeLevel(final Object levelKey, final Object value) {
        final int level;
        try {
            level = Integer.parseInt(String.valueOf(levelKey));
        } catch (NumberFormatException e) {
            return null;
        }
        if (!(value instanceof Map)) {
            return null;
        }
        Map<?, ?> levelMap = (Map<?, ?>) value;
        int upgradePrice = 0;
        Material upgradeMaterial = null;
        Map<Material, Long> intervals = new HashMap<>();
        for (var entry : levelMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (key.equalsIgnoreCase("upgrade") && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> upgradeMap = (Map<String, Object>) entry.getValue();
                upgradePrice = parsePositiveInt(upgradeMap.get("price"), 0);
                String materialName = upgradeMap.get("material") != null ? upgradeMap.get("material").toString() : null;
                upgradeMaterial = materialName != null ? parseUpgradeMaterial(materialName) : null;
            } else if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> intervalMap = (Map<String, Object>) entry.getValue();
                long interval = parsePositiveLong(intervalMap.get("interval"), 0L);
                if (interval > 0L) {
                    Material material = parseUpgradeMaterial(key);
                    if (material != null) {
                        intervals.put(material, interval);
                    }
                }
            }
        }
        if (upgradePrice <= 0 && upgradeMaterial == null && intervals.isEmpty()) {
            return null;
        }
        return new ForgeLevel(level, intervals, upgradePrice, upgradeMaterial);
    }

    private static int parsePositiveInt(final Object value, final int defaultValue) {
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    private static long parsePositiveLong(final Object value, final long defaultValue) {
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    private static @Nullable Material parseUpgradeMaterial(final String name) {
        return switch (name.trim().toLowerCase()) {
            case "iron", "iron_ingot" -> Material.IRON_INGOT;
            case "gold", "gold_ingot" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };
    }
}