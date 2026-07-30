package dev.sebastianjnuwu.bedwars.api.model;

import java.util.Map;

import org.bukkit.Material;

public record ForgeLevel(int level, Map<Material, Long> intervals) {
}
