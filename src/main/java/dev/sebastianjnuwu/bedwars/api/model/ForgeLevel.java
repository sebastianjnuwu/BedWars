package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.Material;

import java.util.Map;

public record ForgeLevel(int level, Map<Material, Long> intervals) {
}
