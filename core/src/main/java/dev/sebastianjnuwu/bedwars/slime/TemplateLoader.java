package dev.sebastianjnuwu.bedwars.slime;

import java.io.File;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carrega e valida templates de SlimeWorld.
 */
public class TemplateLoader {

    private final File templatesFolder;

    /**
     * Cria um novo carregador de templates.
     *
     * @param templatesFolder diretório de templates
     */
    public TemplateLoader(@NotNull File templatesFolder) {
        this.templatesFolder = templatesFolder;
    }

    /**
     * Carrega um template existente.
     *
     * @param name nome do template
     * @return diretório do template ou null se não encontrado
     */
    public @Nullable File loadTemplate(@NotNull String name) {
        final File templateDir = getTemplateFile(name);
        if (!templateDir.exists() || !templateDir.isDirectory()) {
            return null;
        }

        // Verifica se tem level.dat (arquivo obrigatório do mundo)
        final File levelDat = new File(templateDir, "level.dat");
        if (!levelDat.exists()) {
            return null;
        }

        return templateDir;
    }

    /**
     * Valida se um diretório contém um template válido.
     *
     * @param dir diretório do template
     * @return true se válido
     */
    public boolean isValidTemplate(@NotNull File dir) {
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        // Deve ter level.dat
        final File levelDat = new File(dir, "level.dat");
        if (!levelDat.exists() || levelDat.length() == 0) {
            return false;
        }

        // Deve ter folder structure básica
        final File[] requiredDirs = {
            new File(dir, "data"),
            new File(dir, "region"),
            new File(dir, "advancements"),
            new File(dir, "stats")
        };

        for (final File requiredDir : requiredDirs) {
            if (!requiredDir.exists() || !requiredDir.isDirectory()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Lista todos os templates válidos.
     *
     * @return array com nomes dos templates válidos
     */
    public @NotNull String[] listValidTemplates() {
        final File[] files = templatesFolder.listFiles(f -> f.isDirectory());
        if (files == null) {
            return new String[0];
        }

        return java.util.Arrays.stream(files)
                .filter(this::isValidTemplate)
                .map(f -> f.getName())
                .toArray(String[]::new);
    }

    /**
     * Retorna o diretório de um template.
     *
     * @param name nome do template
     * @return diretório
     */
    public @NotNull File getTemplateFile(@NotNull String name) {
        return new File(templatesFolder, name);
    }

    /**
     * Verifica se um template existe.
     *
     * @param name nome do template
     * @return true se existe
     */
    public boolean templateExists(@NotNull String name) {
        return loadTemplate(name) != null;
    }

    /**
     * Remove um template.
     *
     * @param name nome do template
     * @return true se removido
     */
    public boolean removeTemplate(@NotNull String name) {
        final File dir = getTemplateFile(name);
        return deleteFolder(dir);
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
