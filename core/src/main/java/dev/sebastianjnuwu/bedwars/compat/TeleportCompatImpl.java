package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação padrão de teleporte assíncrono usando a API do Paper.
 */
public final class TeleportCompatImpl implements TeleportCompat {

    @Override
    public void teleportAsync(final @NotNull Player player, final @NotNull Location location) {
        player.teleportAsync(location.clone());
    }
}
