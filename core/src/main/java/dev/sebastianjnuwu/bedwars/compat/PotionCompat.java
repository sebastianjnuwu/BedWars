package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Abstrai a aplicação do tipo de poção nos metadados.
 * <p>
 * {@code setBasePotionType(PotionType)} só existe a partir da 1.20.5; em
 * versões anteriores a API usa {@code setBasePotionData(PotionData)}.
 * </p>
 */
public interface PotionCompat {

    /**
     * Aplica o tipo de poção no meta.
     *
     * @param meta      metadados da poção (não nulos)
     * @param potionKey chave do tipo (ex.: {@code poison})
     * @return {@code true} se o tipo foi aplicado com sucesso
     */
    boolean applyPotionType(@NotNull PotionMeta meta, @NotNull String potionKey);
}
