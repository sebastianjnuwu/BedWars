package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Abstrai a aplicação de tags NBT/SNBT cruas em itens.
 * <p>
 * {@code Bukkit.getUnsafe().modifyItemStack(...)} é API do Paper (NBT
 * histórico); em versões onde o SNBT não é suportado pela API, a tag é
 * aplicada de outra forma ou ignorada com segurança.
 * </p>
 */
public interface NbtCompat {

    /**
     * Aplica uma tag SNBT crua ao stack.
     *
     * @param stack item (não nulo)
     * @param tag   tag SNBT (não nula)
     */
    void modifyItemStack(@NotNull ItemStack stack, @NotNull String tag);
}
