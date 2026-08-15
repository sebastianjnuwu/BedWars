package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implementação padrão de registry usando a API 1.19.4+.
 */
public final class RegistryCompatImpl implements RegistryCompat {

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable
    Enchantment getEnchantment(final @NotNull String key) {
        final NamespacedKey namespacedKey = NamespacedKey.minecraft(key.toLowerCase());
        if (namespacedKey == null) {
            return null;
        }
        return Registry.ENCHANTMENT.get(namespacedKey);
    }
}
