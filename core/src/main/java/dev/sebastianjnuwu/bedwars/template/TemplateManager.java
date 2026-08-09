package dev.sebastianjnuwu.bedwars.template;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.slime.SlimeManager;

/**
 * Gerencia templates de SlimeWorld.
 * <p>
 * O TemplateManager é responsável por salvar mundos como templates
 * e carregar templates para criar instâncias de arenas.
 * </p>
 */
public class TemplateManager {

    private final File templatesFolder;
    private final SlimeManager slimeManager;

    /**
     * Cria um novo gerenciador de templates.
     *
     * @param templatesFolder diretório dos templates
     * @param slimeManager gerenciador de SlimeWorld
     */
    public TemplateManager(@NotNull File templatesFolder, @Nullable SlimeManager slimeManager) {
        this.templatesFolder = templatesFolder;
        this.slimeManager = slimeManager;
    }

    /**
     * Salva um mundo como template.
     *
     * @param name nome do template
     * @param world mundo a ser salvo
     * @return CompletableFuture que completa quando o template for salvo
     */
    public @NotNull CompletableFuture<Void> saveTemplate(@NotNull String name, @NotNull World world) {
        if (slimeManager != null) {
            return slimeManager.saveTemplate(name, world);
        }

        return CompletableFuture.runAsync(() -> {
            final File dest = getTemplateFolder(name);
            dest.mkdirs();
            copyWorldFolder(world.getWorldFolder(), dest);
        });
    }

    /**
     * Carrega um template.
     *
     * @param name nome do template
     * @return SlimeWorld ou null se não encontrado
     */
    public @Nullable Object loadTemplate(@NotNull String name) {
        if (slimeManager != null) {
            return slimeManager.loadTemplate(name);
        }
        return null;
    }

    /**
     * Lista todos os templates disponíveis.
     *
     * @return array com nomes dos templates
     */
    public @NotNull String[] listTemplates() {
        final File[] files = templatesFolder.listFiles(f -> f.isDirectory());
        if (files == null) {
            return new String[0];
        }

        return java.util.Arrays.stream(files)
                .map(f -> f.getName())
                .toArray(String[]::new);
    }

    /**
     * Verifica se um template existe.
     *
     * @param name nome do template
     * @return true se existe
     */
    public boolean templateExists(@NotNull String name) {
        return getTemplateFolder(name).exists();
    }

    /**
     * Remove um template.
     *
     * @param name nome do template
     * @return true se removido
     */
    public boolean deleteTemplate(@NotNull String name) {
        final File folder = getTemplateFolder(name);
        return deleteFolder(folder);
    }

    /**
     * Retorna o diretório de um template.
     *
     * @param name nome do template
     * @return diretório
     */
    public @NotNull File getTemplateFolder(@NotNull String name) {
        return new File(templatesFolder, name);
    }

    /**
     * Retorna o diretório de templates.
     *
     * @return diretório
     */
    public @NotNull File getTemplatesFolder() {
        return templatesFolder;
    }

    private void copyWorldFolder(@NotNull File source, @NotNull File target) {
        if (!source.isDirectory()) {
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
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
                copyWorldFolder(file, targetFile);
            } else {
                try {
                    java.nio.file.Files.copy(file.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // Silently skip files that fail to copy
                }
            }
        }
    }

    private boolean isIgnoredFile(@NotNull String name) {
        return name.equals("uid.dat") || name.equals("session.dat") || name.equals("session.lock");
    }

    private boolean deleteFolder(@NotNull File path) {
        if (!path.exists()) {
            return false;
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
        return path.delete();
    }
}
