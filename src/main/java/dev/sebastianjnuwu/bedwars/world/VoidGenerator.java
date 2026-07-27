package dev.sebastianjnuwu.bedwars.world;

import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Gerador de mundo vazio (void).
 * Não gera nenhum bloco, apenas o ar.
 */
public class VoidGenerator extends ChunkGenerator {

    @Override
    public void generateSurface(
            final @NotNull WorldInfo worldInfo,
            final @NotNull Random random,
            final int chunkX,
            final int chunkZ,
            final @NotNull ChunkData chunkData
    ) {
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldGenerateBedrock() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }
}

