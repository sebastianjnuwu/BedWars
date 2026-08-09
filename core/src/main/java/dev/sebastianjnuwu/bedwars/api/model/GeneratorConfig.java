package dev.sebastianjnuwu.bedwars.api.model;

import java.util.Map;

import org.bukkit.Material;

public record GeneratorConfig(Material material, Map<Integer, Long> levels) {

    public long intervalForLevel(final int level) {
        if (this.levels == null || this.levels.isEmpty()) {
            return 0L;
        }
        final Long direct = this.levels.get(level);
        if (direct != null) {
            return direct;
        }
        int bestLevel = -1;
        for (final var entry : this.levels.entrySet()) {
            if (entry.getKey() <= level && entry.getKey() > bestLevel) {
                bestLevel = entry.getKey();
            }
        }
        if (bestLevel != -1) {
            return this.levels.get(bestLevel);
        }
        int minLevel = Integer.MAX_VALUE;
        for (final int key : this.levels.keySet()) {
            if (key < minLevel) {
                minLevel = key;
            }
        }
        return this.levels.get(minLevel);
    }
}
