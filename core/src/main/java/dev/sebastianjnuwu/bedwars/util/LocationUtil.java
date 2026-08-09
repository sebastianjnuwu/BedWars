package dev.sebastianjnuwu.bedwars.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;

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
     * Teleporta o jogador de forma assíncrona usando a compatibilidade de
     * teleporte para evitar travamento.
     */
    public static void safeTeleportAsync(final @NotNull Player player, final @Nullable Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        CompatProvider.teleport().teleportAsync(player, location.clone());
    }

    /**
     * Encontra um ponto de respawn seguro próximo ao local de origem.
     * <p>
     * Um local é considerado seguro quando o jogador tem dois blocos de ar
     * (corpo e cabeça) e um bloco sólido sob os pés. A busca é feita em espiral
     * a partir da coluna original (até 4 blocos de raio) e, dentro de cada
     * coluna, para cima até o teto do mundo. Isso evita renascer dentro de uma
     * parede ou colado a um bloco (dano de sufocação). O ponto retornado é
     * centralizado no bloco (x/z + 0.5) para que o corpo não atravesse blocos
     * laterais.
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
        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int y = baseY; y <= maxY; y++) {
                        if (isSafeSpot(world, x + dx, y, z + dz)) {
                            return centered(world, x + dx, y, z + dz, origin);
                        }
                    }
                }
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

    private static @NotNull Location centered(final @NotNull World world, final int x, final int y, final int z, final @NotNull Location origin) {
        return new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
    }
}
