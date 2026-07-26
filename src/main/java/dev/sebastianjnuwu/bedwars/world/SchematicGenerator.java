package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitWorld;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;

/**
 * Copia exatamente como o SkyWarsReloaded faz - usa FAWE/WorldEdit para colar schematic.
 */
public class SchematicGenerator {

    private final File schematicFile;
    private final World world;
    private final Location location;
    private final Runnable onComplete;

    public SchematicGenerator(
            final @NotNull File schematicFile,
            final @NotNull World world,
            final @NotNull Location location,
            final @NotNull Runnable onComplete
    ) {
        this.schematicFile = schematicFile;
        this.world = world;
        this.location = location;
        this.onComplete = onComplete;
    }

    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                pasteSchematic();
                onComplete.run();
            }
        }.runTask(BedWarsPlugin.getPlugin(BedWarsPlugin.class));
    }

    private void pasteSchematic() {
        try {
            Clipboard clipboard;
            ClipboardFormat format = ClipboardFormats.findByFile(schematicFile);

            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                clipboard = reader.read();
            }

            // Usar FAWE queue para processar em segundo plano sem travar
            final com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);
            try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                final Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                        .ignoreAirBlocks(false)
                        .build();
                
                // Executar a operação (FAWE já é rápido o suficiente)
                Operations.complete(operation);
            } catch (WorldEditException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
