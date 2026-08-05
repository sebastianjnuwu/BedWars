package dev.sebastianjnuwu.bedwars.api.model;

import java.util.Map;

import org.bukkit.Material;

public record ForgeLevel(int level, Map<Material, Long> intervals, int upgradePrice, Material upgradeMaterial) {

    public ForgeLevel(int level, Map<Material, Long> intervals) {
        this(level, intervals, 0, null);
    }
}
