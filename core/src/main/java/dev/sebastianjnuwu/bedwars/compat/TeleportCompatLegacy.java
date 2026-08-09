package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Implementação legada de teleporte para versões anteriores ao Paper 1.20
 * (onde {@code Player#teleportAsync(Location)} não existe). Usa o teleporte
 * síncrono padrão do Bukkit, disponível em todas as versões.
 */
public final class TeleportCompatLegacy implements TeleportCompat {

    @Override
    public void teleportAsync(final @NotNull Player player, final @NotNull Location location) {
        player.teleport(location.clone());
    }
}
