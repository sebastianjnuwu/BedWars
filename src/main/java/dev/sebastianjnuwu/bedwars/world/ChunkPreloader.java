package dev.sebastianjnuwu.bedwars.world;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;

/**
 * Carrega chunks ao redor de uma posição central para evitar lag ao aproximar.
 * Processa um número fixo de chunks por tick.
 */
public class ChunkPreloader {

    private final World world;
    private final int centerX;
    private final int centerZ;
    private final int radius;
    private final int chunksPerTick;
    private final Runnable onComplete;

    private int loadedChunks = 0;
    private int totalChunks = 0;

    /**
     * Cria um carregador de chunks.
     *
     * @param world mundo onde carregar
     * @param centerLoc centro da área a ser carregada
     * @param radius raio em chunks (ex: 10 = 160 blocos)
     * @param chunksPerTick chunks a carregar por tick
     * @param onComplete callback quando terminar
     */
    public ChunkPreloader(
            final @NotNull World world,
            final @NotNull Location centerLoc,
            final int radius,
            final int chunksPerTick,
            final @NotNull Runnable onComplete
    ) {
        this.world = world;
        this.centerX = centerLoc.getBlockX() >> 4;
        this.centerZ = centerLoc.getBlockZ() >> 4;
        this.radius = radius;
        this.chunksPerTick = chunksPerTick;
        this.onComplete = onComplete;
        this.totalChunks = (2 * radius + 1) * (2 * radius + 1);
    }

    /**
     * Inicia o carregamento dos chunks.
     */
    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                int loadedThisTick = 0;
                
                // Carregar chunks em espiral a partir do centro
                for (int dx = -radius; dx <= radius && loadedThisTick < chunksPerTick; dx++) {
                    for (int dz = -radius; dz <= radius && loadedThisTick < chunksPerTick; dz++) {
                        if (dx * dx + dz * dz <= radius * radius) {
                            final int cx = centerX + dx;
                            final int cz = centerZ + dz;
                            
                            if (!isChunkLoaded(cx, cz)) {
                                world.getChunkAt(cx, cz);
                                loadedChunks++;
                                loadedThisTick++;
                            }
                        }
                    }
                }
                
                if (loadedChunks >= totalChunks) {
                    this.cancel();
                    onComplete.run();
                }
            }
        }.runTaskTimer(BedWarsPlugin.getPlugin(BedWarsPlugin.class), 1L, 1L);
    }

    private boolean isChunkLoaded(final int cx, final int cz) {
        final Chunk chunk = world.getChunkAt(cx, cz);
        return chunk.isLoaded();
    }

    public double getProgress() {
        if (totalChunks == 0) return 1.0;
        return (double) loadedChunks / totalChunks;
    }

    public int getLoadedChunks() {
        return loadedChunks;
    }

    public int getTotalChunks() {
        return totalChunks;
    }
}
