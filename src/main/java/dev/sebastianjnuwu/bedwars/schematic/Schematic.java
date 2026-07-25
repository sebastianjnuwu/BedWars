package dev.sebastianjnuwu.bedwars.schematic;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representa um schematic (cópia de uma região de blocos).
 * Pode ser salvo em arquivo e carregado para colar em qualquer mundo.
 */
public class Schematic {

    private final String name;
    private final int width;
    private final int height;
    private final int length;
    private final List<BlockData> blocks;
    private final List<int[]> positions;

    /**
     * Cria um schematic a partir de uma região do mundo.
     *
     * @param name nome do schematic
     * @param pos1 primeira posição
     * @param pos2 segunda posição
     */
    public Schematic(final String name, final @NotNull Location pos1, final @NotNull Location pos2) {
        this.name = name;

        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        this.width = maxX - minX + 1;
        this.height = maxY - minY + 1;
        this.length = maxZ - minZ + 1;

        final World world = pos1.getWorld();
        this.blocks = new ArrayList<>();
        this.positions = new ArrayList<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    final Block block = world.getBlockAt(x, y, z);
                    if (!block.getType().isAir()) {
                        this.blocks.add(block.getBlockData());
                        this.positions.add(new int[]{
                                x - minX,
                                y - minY,
                                z - minZ
                        });
                    }
                }
            }
        }
    }

    /**
     * Cria um schematic a partir de dados carregados.
     */
    private Schematic(
            final String name,
            final int width,
            final int height,
            final int length,
            final List<BlockData> blocks,
            final List<int[]> positions
    ) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = blocks;
        this.positions = positions;
    }

    /**
     * Cola o schematic no mundo a partir da posição base.
     * Otimizado: agrupa blocos por chunk e pré-carrega os chunks.
     *
     * @param base localização base (canto mínimo)
     */
    public void paste(final @NotNull Location base) {
        final World world = base.getWorld();
        final int baseX = base.getBlockX();
        final int baseY = base.getBlockY();
        final int baseZ = base.getBlockZ();

        final Map<Long, List<Integer>> chunkMap = new HashMap<>();
        for (int i = 0; i < this.positions.size(); i++) {
            final int[] pos = this.positions.get(i);
            final int cx = (baseX + pos[0]) >> 4;
            final int cz = (baseZ + pos[2]) >> 4;
            final long key = (long) cx << 32 | (cz & 0xFFFFFFFFL);
            chunkMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        for (final long key : chunkMap.keySet()) {
            final int cx = (int) (key >> 32);
            final int cz = (int) key;
            world.getChunkAt(cx, cz);
        }

        for (final var entry : chunkMap.entrySet()) {
            final long key = entry.getKey();
            final int cx = (int) (key >> 32);
            final int cz = (int) key;
            final Chunk chunk = world.getChunkAt(cx, cz);

            for (final int i : entry.getValue()) {
                final int[] pos = this.positions.get(i);
                final int wx = baseX + pos[0];
                final int wz = baseZ + pos[2];
                chunk.getBlock(wx & 0xF, baseY + pos[1], wz & 0xF)
                        .setBlockData(this.blocks.get(i), false);
            }
        }
    }

    /**
     * Salva o schematic em um arquivo.
     *
     * @param file arquivo de destino
     * @throws IOException se houver erro de escrita
     */
    public void save(final @NotNull File file) throws IOException {
        try (final DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeUTF(this.name);
            out.writeInt(this.width);
            out.writeInt(this.height);
            out.writeInt(this.length);
            out.writeInt(this.blocks.size());

            for (int i = 0; i < this.blocks.size(); i++) {
                final int[] pos = this.positions.get(i);
                out.writeInt(pos[0]);
                out.writeInt(pos[1]);
                out.writeInt(pos[2]);
                out.writeUTF(this.blocks.get(i).getAsString());
            }
        }
    }

    /**
     * Carrega um schematic de um arquivo.
     *
     * @param file arquivo de origem
     * @return schematic carregado
     * @throws IOException se houver erro de leitura
     */
    public static Schematic load(final @NotNull File file) throws IOException {
        try (final DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            final String name = in.readUTF();
            final int width = in.readInt();
            final int height = in.readInt();
            final int length = in.readInt();
            final int blockCount = in.readInt();

            final List<BlockData> blocks = new ArrayList<>(blockCount);
            final List<int[]> positions = new ArrayList<>(blockCount);

            for (int i = 0; i < blockCount; i++) {
                final int x = in.readInt();
                final int y = in.readInt();
                final int z = in.readInt();
                final String data = in.readUTF();
                positions.add(new int[]{x, y, z});
                blocks.add(Bukkit.createBlockData(data));
            }

            return new Schematic(name, width, height, length, blocks, positions);
        }
    }

    /**
     * Retorna a lista de strings de BlockData.
     */
    public List<String> getBlockDataStrings() {
        final List<String> list = new ArrayList<>();
        for (final BlockData data : this.blocks) {
            list.add(data != null ? data.getAsString() : "minecraft:air");
        }
        return list;
    }

    public String getName() {
        return this.name;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLength() {
        return this.length;
    }
}
