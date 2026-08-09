package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Abstrai o teleporte assíncrono.
 * <p>
 * {@code Player#teleportAsync(Location)} é API do Paper; no Spigot o fallback
 * é o teleporte síncrono na main thread.
 * </p>
 */
public interface TeleportCompat {

    /**
     * Teleporta o jogador para o local.
     *
     * @param player   jogador (não nulo)
     * @param location destino (não nulo)
     */
    void teleportAsync(@NotNull Player player, @NotNull Location location);
}
