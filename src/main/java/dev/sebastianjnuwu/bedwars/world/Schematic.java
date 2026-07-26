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
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
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
        
        final int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        final int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        final int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
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
    private Schematic(final String name, final int width, final int height, final int length) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
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

    /**
     * Salva o schematic em formato .schem (FAWE).
     * O FAWE já fornece o comando //save que salva o schematic atual.
     * Este método é apenas para compatibilidade - o usuário deve usar //save do FAWE.
     *
     * @param file arquivo de destino
     */
    public void save(final @NotNull File file) throws IOException {
        // O FAWE salva o schematic automaticamente com //save
        // Este método é apenas para compatibilidade
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
            
            return new Schematic(name, width, height, length);
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
