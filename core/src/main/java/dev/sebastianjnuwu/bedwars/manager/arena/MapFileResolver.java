package dev.sebastianjnuwu.bedwars.manager.arena;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Resolve o arquivo de mapa (schematic) usado por uma arena.
 * <p>
 * Considera o mapa compartilhado ({@link Arena#getMapName()}) quando
 * configurado, permitindo que várias arenas rodem partidas simultâneas do
 * mesmo mapa, e prioriza o formato interno {@code .bwmap}.
 * </p>
 */
final class MapFileResolver {

    private final File mapsFolder;

    MapFileResolver(@NotNull final File mapsFolder) {
        this.mapsFolder = mapsFolder;
    }

    /**
     * Resolve o arquivo de mapa usado por uma arena.
     *
     * @param arena arena cujo mapa deve ser resolvido (não nula)
     * @return arquivo do schematic, ou {@code null} se não encontrado
     */
    @Nullable File forArena(@NotNull final Arena arena) {
        final String mapName = arena.getMapName();
        final String resolved = mapName == null || mapName.isBlank() ? arena.getName() : mapName;
        return byName(resolved);
    }

    /**
     * Resolve o arquivo de mapa de uma arena pelo nome.
     *
     * @param name nome da arena
     * @return arquivo do schematic ou {@code null}
     */
    @Nullable File byName(@NotNull final String name) {
        File file = new File(this.mapsFolder, name + ".bwmap");
        if (!file.exists()) {
            file = new File(this.mapsFolder, name + ".schem");
        }
        if (!file.exists()) {
            file = new File(this.mapsFolder, name + ".schematic");
        }
        if (!file.exists()) {
            file = new File(this.mapsFolder, name + ".nbt");
        }
        if (!file.exists()) {
            file = new File(this.mapsFolder, name);
        }
        if (file.exists()) {
            return file;
        }
        final File[] files = this.mapsFolder.listFiles();
        if (files != null) {
            for (final File candidate : files) {
                if (candidate.isFile() && stripMapExtension(candidate.getName()).equalsIgnoreCase(name)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static String stripMapExtension(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
