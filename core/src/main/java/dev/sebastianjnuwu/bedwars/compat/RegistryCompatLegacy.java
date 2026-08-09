package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementação legada de registry para versões anteriores à introdução do
 * {@code org.bukkit.Registry} (1.19.4). Usa {@code Enchantment.getByName},
 * disponível em todas as versões.
 */
public final class RegistryCompatLegacy implements RegistryCompat {

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable
    Enchantment getEnchantment(final @NotNull String key) {
        return Enchantment.getByName(key.toUpperCase());
    }
}
