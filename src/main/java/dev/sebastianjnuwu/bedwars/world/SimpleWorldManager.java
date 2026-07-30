package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Implementação simples de SlimeWorldManager usando cópia de diretórios de mundo.
 * Usa o sistema nativo do Bukkit para criar e carregar mundos.
 * <p>
 * Nota: Quando AdvancedSlimeWorldManager estiver disponível, essa classe pode ser
 * substituída por uma implementação que use SlimeWorld diretamente.
 * </p>
 */
@SuppressWarnings("deprecation")
public class SimpleWorldManager implements
 SlimeWorldManager {

    private static final Set<String> IGNORED_FILES = new HashSet<>();

    static {
        IGNORED_FILES.add("uid.dat");
        IGNORED_FILES.add("session.dat");
        IGNORED_FILES.add("session.lock");
    }

    private final File templatesFolder;
    private final File instancesFolder;
    private final File pluginFolder;

    /**
     * Cria um novo gerenciador de mundos.
     *
     * @param pluginFolder diretório do plugin
     */
    public SimpleWorldManager(@NotNull File pluginFolder) {
        this.pluginFolder = pluginFolder;
        this.templatesFolder = new File(pluginFolder, "templates");
        this.templatesFolder.mkdirs();
        this.instancesFolder = new File(Bukkit.getWorldContainer(), "bedwars_instances");
        this.instancesFolder.mkdirs();
    }

    @Override
    public void saveTemplate(@NotNull String name, @NotNull World world) throws IOException {
        final File dest = getTemplateFolder(name);
        deleteFolder(dest);
        dest.mkdirs();
        copyFolder(world.getWorldFolder(), dest);
    }

    @Override
    public boolean templateExists(@NotNull String name) {
        final File folder = getTemplateFolder(name);
        return folder.exists() && folder.isDirectory() && new File(folder, "level.dat").exists();
    }

    @Override
    public @Nullable World createInstance(@NotNull String templateName, @NotNull String instanceName) throws IOException {
        final File template = getTemplateFolder(templateName);
        if (!template.exists() || !template.isDirectory()) {
            throw new IOException("Template não encontrado: " + templateName);
        }

        final File instanceFolder = getInstanceFolder(instanceName);
        deleteFolder(instanceFolder);

        // Copia o template para a instância
        copyFolder(template, instanceFolder);

        // Carrega o mundo
        return loadInstance(instanceName);
    }

    @Override
    public @Nullable World loadInstance(@NotNull String name) {
        // Verifica se já está carregado
        World world = Bukkit.getWorld(name);
        if (world != null) {
            return world;
        }

        // Verifica se a pasta existe
        final File instanceFolder = getInstanceFolder(name);
        if (!instanceFolder.exists() || !new File(instanceFolder, "level.dat").exists()) {
            return null;
        }

        // Cria e carrega o mundo
        final WorldCreator wc = new WorldCreator(name);
        wc.generator((ChunkGenerator) null);
        wc.generateStructures(false);
        wc.environment(World.Environment.NORMAL);

        world = wc.createWorld();

        if (world != null) {
            // Configurações padrão para arenas
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);
            world.setSpawnFlags(true, true);
            world.setPVP(true);
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setAutoSave(true);
            world.setKeepSpawnInMemory(false);
            world.setTicksPerAnimalSpawns(1);
            world.setTicksPerMonsterSpawns(1);

            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(org.bukkit.GameRule.SHOW_DEATH_MESSAGES, false);
        }

        return world;
    }

    @Override
    public boolean unloadInstance(@NotNull String name) {
        final World world = Bukkit.getWorld(name);
        if (world == null) {
            return false;
        }

        // Teleporta jogadores para o lobby
        for (final Player player : world.getPlayers()) {
            player.kick(Component.text(org.bukkit.plugin.java.JavaPlugin.getPlugin(BedWarsPlugin.class).getLang().raw("kick.arena_reset")));
        }

        return Bukkit.unloadWorld(world, true);
    }

    @Override
    public void deleteInstance(@NotNull String name) {
        unloadInstance(name);
        final File folder = getInstanceFolder(name);
        deleteFolder(folder);
    }

    @Override
    public boolean isInstanceLoaded(@NotNull String name) {
        return Bukkit.getWorld(name) != null;
    }

    @Override
    public @NotNull File getTemplatesFolder() {
        return templatesFolder;
    }

    @Override
    public @NotNull File getInstancesFolder() {
        return instancesFolder;
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
     * Retorna o diretório de uma instância.
     *
     * @param name nome da instância
     * @return diretório
     */
    public @NotNull File getInstanceFolder(@NotNull String name) {
        return new File(instancesFolder, name);
    }

    /**
     * Retorna o nome da instância a partir da arena e UUID.
     *
     * @param arena arena
     * @param uuid UUID único
     * @return nome da instância
     */
    public @NotNull String getInstanceName(@NotNull Arena arena, @NotNull UUID uuid) {
        // Formato: bw-arenauuid
        return String.format("bw-%s-%s", arena.getName(), uuid.toString().substring(0, 8));
    }

    private void deleteFolder(@NotNull File path) {
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

    private void copyFolder(@NotNull File source, @NotNull File target) throws IOException {
        if (!source.isDirectory()) {
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
            throw new IOException("Não foi possível criar diretório: " + target);
        }

        final File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (IGNORED_FILES.contains(file.getName())) {
                continue;
            }

            final File targetFile = new File(target, file.getName());
            if (file.isDirectory()) {
                copyFolder(file, targetFile);
            } else {
                copyFile(file, targetFile);
            }
        }
    }

    private void copyFile(@NotNull File source, @NotNull File target) throws IOException {
        if (IGNORED_FILES.contains(source.getName())) {
            return;
        }

        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}
