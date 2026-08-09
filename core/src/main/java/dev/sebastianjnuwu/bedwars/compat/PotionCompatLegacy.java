package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação legada de poções para versões anteriores à 1.20.5 (antes de
 * {@code PotionMeta#setBasePotionType(PotionType)}). Usa
 * {@code setBasePotionData(PotionData)}, disponível em todas as versões.
 */
public final class PotionCompatLegacy implements PotionCompat {

    @SuppressWarnings("deprecation")
    @Override
    public boolean applyPotionType(final @NotNull PotionMeta meta, final @NotNull String potionKey) {
        final String simple = potionKey.contains(":") ? potionKey.substring(potionKey.indexOf(':') + 1) : potionKey;
        final PotionType type;
        try {
            type = PotionType.valueOf(simple.toUpperCase());
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
        meta.setBasePotionData(new PotionData(type, false, false));
        return true;
    }
}
