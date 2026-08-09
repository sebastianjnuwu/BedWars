package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Abstrai a resolução de encantamentos por chave.
 * <p>
 * {@code Registry.ENCHANTMENT} só existe a partir da 1.19.4; em versões
 * anteriores a API usa {@code Enchantment.getByKey(NamespacedKey)}.
 * </p>
 */
public interface RegistryCompat {

    /**
     * Retorna o encantamento correspondente à chave (ex.: {@code sharpness}).
     *
     * @param key chave do encantamento (não nula)
     * @return encantamento ou {@code null} se não existir
     */
    @Nullable
    Enchantment getEnchantment(@NotNull String key);
}
