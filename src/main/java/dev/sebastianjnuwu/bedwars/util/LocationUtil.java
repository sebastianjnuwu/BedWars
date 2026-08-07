package dev.sebastianjnuwu.bedwars.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

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
     * Teleporta o jogador de forma segura.
     */
    public static void safeTeleport(final @NotNull Player player, final @Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        player.teleport(location.clone());
    }

    /**
     * Teleporta o jogador de forma assíncrona usando teleportAsync para evitar travamento.
     */
    public static void safeTeleportAsync(final @NotNull Player player, final @Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        player.teleportAsync(location.clone());
    }

    /**
     * Encontra um ponto de respawn seguro próximo ao local de origem.
     * <p>
     * Um local é considerado seguro quando o jogador tem dois blocos de ar
     * (corpo e cabeça) e um bloco sólido sob os pés. Se o ponto original
     * estiver ocupado por blocos (morte por sufocamento no respawn), procura
     * para cima na coluna até o teto do mundo.
     * </p>
     *
     * @param origin local de origem (spawn do time)
     * @return local seguro ou o próprio original se não encontrar
     */
    public static @NotNull Location findSafeRespawn(final @Nullable Location origin) {
        if (origin == null || origin.getWorld() == null) {
            return origin;
        }
        final World world = origin.getWorld();
        final int x = origin.getBlockX();
        final int baseY = origin.getBlockY();
        final int z = origin.getBlockZ();
        final int maxY = Math.min(world.getMaxHeight() - 2, baseY + 64);
        for (int y = baseY; y <= maxY; y++) {
            if (isSafeSpot(world, x, y, z)) {
                return withY(origin, y);
            }
        }
        return origin.clone();
    }

    private static boolean isSafeSpot(final World world, final int x, final int y, final int z) {
        final Block feet = world.getBlockAt(x, y, z);
        final Block head = world.getBlockAt(x, y + 1, z);
        if (!feet.isPassable() || !head.isPassable()) {
            return false;
        }
        return !world.getBlockAt(x, y - 1, z).isPassable();
    }

    private static @NotNull Location withY(final @NotNull Location origin, final int y) {
        final Location safe = origin.clone();
        safe.setY(y);
        return safe;
    }
}
