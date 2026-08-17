package dev.sebastianjnuwu.bedwars.slime;

import java.io.File;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;

/**
 * Operações de arquivo do sistema Slime: cópia recursiva de mundos, exclusão
 * de pastas e filtro de arquivos ignorados.
 */
final class SlimeFileUtil {

    private SlimeFileUtil() {
    }

    /**
     * Copia recursivamente o conteúdo de uma pasta de mundo para o destino,
     * ignorando arquivos de bloqueio do servidor.
     *
     * @param plugin plugin BedWars (para logs)
     * @param source pasta de origem
     * @param target pasta de destino
     */
    static void copyWorldFolder(@NotNull Plugin plugin, @NotNull File source, @NotNull File target) {
        if (!source.isDirectory()) {
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
            plugin.getLogger().severe(((BedWarsPlugin) plugin).getLang().raw("log.slime_manager.directory_create_error", target));
            return;
        }

        final File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (isIgnoredFile(file.getName())) {
                continue;
            }

            final File targetFile = new File(target, file.getName());
            if (file.isDirectory()) {
                copyWorldFolder(plugin, file, targetFile);
            } else {
                try {
                    java.nio.file.Files.copy(file.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e) {
                    plugin.getLogger().warning(((BedWarsPlugin) plugin).getLang().raw("log.slime_manager.copy_error", file.getName(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Exclui recursivamente uma pasta.
     *
     * @param path pasta a excluir
     */
    static void deleteFolder(@NotNull File path) {
        if (!path.exists()) {
            return;
        }

        final File[] files = path.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

    private static boolean isIgnoredFile(@NotNull String name) {
        return name.equals("uid.dat") || name.equals("session.dat") || name.equals("session.lock");
    }
}