package dev.sebastianjnuwu.bedwars.util;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utilitários para teleporte seguro e resolução de posições de arena.
 */
public final class LocationUtil {

    private LocationUtil() {
    }

    public static @NotNull Location getPasteLocation(final @NotNull Arena arena, final @NotNull World world) {
        if (arena.getPasteX() != 0 || arena.getPasteY() != 0 || arena.getPasteZ() != 0) {
            return new Location(world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
        }
        return world.getSpawnLocation();
    }

    public static @Nullable Location getEditTeleportLocation(final @NotNull Arena arena, final @NotNull World world) {
        if (arena.getArenaSpawn() != null && arena.getArenaSpawn().getWorld() != null) {
            return arena.getArenaSpawn();
        }
        return getPasteLocation(arena, world);
    }

    /**
     * Teleporta o jogador e coloca vidro abaixo dos pés se não houver bloco sólido.
     */
    public static void safeTeleport(final @NotNull Player player, final @Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        final Location target = location.clone();
        ensureSolidBelow(target);
        player.teleport(target);
    }

    private static void ensureSolidBelow(final @NotNull Location location) {
        final Block below = location.getBlock().getRelative(0, -1, 0);
        if (!below.getType().isSolid()) {
            below.setType(Material.GLASS, false);
        }
    }
}
