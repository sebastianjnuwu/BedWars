package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação padrão de poções usando a API 1.20.5+.
 */
public final class PotionCompatImpl implements PotionCompat {

    @Override
    public boolean applyPotionType(final @NotNull PotionMeta meta, final @NotNull String potionKey) {
        final String simple = potionKey.contains(":") ? potionKey.substring(potionKey.indexOf(':') + 1) : potionKey;
        final PotionType type;
        try {
            type = PotionType.valueOf(simple.toUpperCase());
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
        meta.setBasePotionType(type);
        return true;
    }
}
