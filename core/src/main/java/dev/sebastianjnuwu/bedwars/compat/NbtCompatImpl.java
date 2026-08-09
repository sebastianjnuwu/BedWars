package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação padrão de NBT usando {@code Bukkit.getUnsafe().modifyItemStack}
 * do Paper.
 */
public final class NbtCompatImpl implements NbtCompat {

    @Override
    public void modifyItemStack(final @NotNull ItemStack stack, final @NotNull String tag) {
        try {
            Bukkit.getUnsafe().modifyItemStack(stack, tag);
        } catch (final Exception ignored) {
        }
    }
}
