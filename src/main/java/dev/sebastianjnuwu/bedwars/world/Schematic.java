package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Representa um schematic usando FAWE/WorldEdit para colagem rápida.
 * Usa formato .schem (padrão FAWE) para salvar e carregar schematics.
 */
public class Schematic {

    private final String name;
    private final int width;
    private final int height;
    private final int length;
    private final int minX;
    private final int minY;
    private final int minZ;

    /**
     * Cria um schematic a partir de uma região do mundo usando FAWE.
     * Use //pos1 e //pos2 do FAWE para selecionar a área.
     *
     * @param name nome do schematic
     * @param pos1 primeira posição
     * @param pos2 segunda posição
     */
    public Schematic(final String name, final @NotNull Location pos1, final @NotNull Location pos2) {
        this.name = name;

        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        final int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        final int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        final int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        this.width = maxX - minX + 1;
        this.height = maxY - minY + 1;
        this.length = maxZ - minZ + 1;
    }

    /**
     * Cria um schematic a partir de arquivo .schem (FAWE).
     *
     * @param name nome do schematic
     * @param schematicFile arquivo .schem
     */
    private Schematic(final String name, final int width, final int height, final int length,
                     final int minX, final int minY, final int minZ) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
    }

    /**
     * Cola o schematic no mundo usando FAWE clipboard (muito mais rápido).
     *
     * @param world mundo de destino
     * @param location localização base
     * @param schematicFile arquivo .schem original
     * @throws WorldEditException se houver erro
     */
    public void paste(final @NotNull World world, final @NotNull Location location, final @NotNull File schematicFile) throws WorldEditException, IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

        try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
            Clipboard clipboard = reader.read();

            com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                        .ignoreAirBlocks(false)
                        .build();
                
                Operations.complete(operation);
            }
        }
    }

    public void save(final @NotNull File file, final @NotNull World world) throws IOException {
        final com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        final BlockVector3 min = BlockVector3.at(this.minX, this.minY, this.minZ);
        final BlockVector3 max = BlockVector3.at(
                this.minX + this.width - 1,
                this.minY + this.height - 1,
                this.minZ + this.length - 1
        );
        final CuboidRegion region = new CuboidRegion(weWorld, min, max);

        ClipboardFormat format = ClipboardFormats.findByAlias("sponge");
        if (format == null) {
            format = ClipboardFormats.findByAlias("schem");
        }
        if (format == null) {
            format = ClipboardFormats.findByAlias("schematic");
        }
        if (format == null) {
            final java.util.Collection<ClipboardFormat> all = ClipboardFormats.getAll();
            if (!all.isEmpty()) {
                format = all.iterator().next();
            }
        }
        if (format == null) {
            throw new IOException("Nenhum formato de schematic registrado no FAWE");
        }

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            final Clipboard clipboard = new BlockArrayClipboard(region, java.util.UUID.randomUUID());
            final ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.completeLegacy(copy);

            try (ClipboardWriter writer = format.getWriter(new java.io.FileOutputStream(file))) {
                writer.write(clipboard);
            }
        } catch (final WorldEditException e) {
            throw new IOException("Erro ao capturar/salvar schematic: " + e.getMessage(), e);
        }
    }

    /**
     * Carrega um schematic de arquivo .schem.
     *
     * @param name nome do schematic
     * @param file arquivo .schem
     * @return schematic carregado
     * @throws IOException se houver erro
     */
    public static Schematic load(final @NotNull String name, final @NotNull File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("Arquivo de mapa não encontrado: " + file.getName());
        }
        
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            throw new IOException("Formato de schematic inválido: " + file.getName());
        }
        
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            BlockVector3 min = clipboard.getMinimumPoint();
            BlockVector3 max = clipboard.getMaximumPoint();

            int width = max.getX() - min.getX() + 1;
            int height = max.getY() - min.getY() + 1;
            int length = max.getZ() - min.getZ() + 1;

            return new Schematic(name, width, height, length,
                    min.getBlockX(), min.getBlockY(), min.getBlockZ());
        }
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
