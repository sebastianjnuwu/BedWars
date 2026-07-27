package dev.sebastianjnuwu.bedwars.slime;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Gerencia mundos usando AdvancedSlimePaper/SlimeWorld como sistema principal.
 * FAWE deve ser usado apenas para criação de mapas, não para carregamento de arenas.
 * <p>
 * API Reference: https://infernalsuite.com/docs/asp/
 */
public class SlimeManager {

    private final Plugin plugin;
    private final File templatesFolder;
    private final File instancesFolder;

    /**
     * Cria um novo gerenciador de SlimeWorld.
     *
     * @param plugin plugin BedWars
     */
    public SlimeManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        this.templatesFolder.mkdirs();
        this.instancesFolder = new File(Bukkit.getWorldContainer(), "bedwars_instances");
        this.instancesFolder.mkdirs();
    }

    /**
     * Verifica se AdvancedSlimePaper está instalado e ativo.
     *
     * @return true se disponível
     */
    public boolean isAvailable() {
        try {
            AdvancedSlimePaperAPI.instance();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Salva um mundo como template Slime.
     * Usado após a criação do mapa com FAWE.
     *
     * @param name nome do template
     * @param world mundo original
     * @return CompletableFuture que completa quando o template for salvo
     */
    public @NotNull CompletableFuture<Void> saveTemplate(@NotNull String name, @NotNull World world) {
        return CompletableFuture.runAsync(() -> {
            final File dest = getTemplateFolder(name);
            dest.mkdirs();

            // Copia os arquivos do mundo para o template
            copyWorldFolder(world.getWorldFolder(), dest);

            plugin.getLogger().info("Template salvo: " + name);
        });
    }

    /**
     * Carrega um template Slime usando o file loader.
     *
     * @param name nome do template
     * @return SlimeWorld ou null se não encontrado
     */
    public @Nullable SlimeWorld loadTemplate(@NotNull String name) {
        if (!isAvailable()) {
            return null;
        }

        final File templateFolder = getTemplateFolder(name);
        if (!templateFolder.exists() || !new File(templateFolder, "level.dat").exists()) {
            return null;
        }

        try {
            final AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
            final FileLoader loader = new FileLoader(templatesFolder);

            return api.readVanillaWorld(templateFolder, name, loader);

        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao carregar template " + name + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria um mundo Bukkit a partir de um SlimeWorld.
     *
     * @param slimeWorld SlimeWorld
     * @return Mundo Bukkit
     */
    public @Nullable World createBukkitWorld(@NotNull SlimeWorld slimeWorld) {
        if (!isAvailable()) {
            return null;
        }

        try {
            final AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
            final SlimeWorldInstance worldInstance = api.loadWorld(slimeWorld, true);

            // Aguarda o mundo ser carregado
            int retries = 20;
            World world = null;
            while (retries-- > 0 && (world = Bukkit.getWorld(slimeWorld.getName())) == null) {
                Thread.sleep(50);
            }

            return world;
        } catch (Exception e) {
            plugin.getLogger().severe("Erro ao criar mundo Bukkit: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria uma nova instância a partir de um template.
     *
     * @param templateName nome do template
     * @param arena arena (para gerar nome único)
     * @return CompletableFuture com o mundo instanciado
     */
    public @NotNull CompletableFuture<World> createInstance(@NotNull String templateName, @NotNull Arena arena) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isAvailable()) {
                throw new IllegalStateException("AdvancedSlimePaper não está disponível");
            }

            final SlimeWorld templateWorld = loadTemplate(templateName);
            if (templateWorld == null) {
                throw new IllegalStateException("Template não encontrado: " + templateName);
            }

            // Gera um nome único para a instância
            final String instanceName = "bw-" + arena.getName() + "-" + UUID.randomUUID().toString().substring(0, 8);

            try {
                // Clona o mundo (temporário, sem loader persistente)
                final SlimeWorld clonedWorld = templateWorld.clone(instanceName);

                // Carrega o mundo no servidor
                final AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
                final SlimeWorldInstance worldInstance = api.loadWorld(clonedWorld, true);

                // Aguarda o mundo ser carregado
                int retries = 20; // 1 segundo de espera máxima
                World world = null;
                while (retries-- > 0 && (world = Bukkit.getWorld(instanceName)) == null) {
                    Thread.sleep(50);
                }

                if (world == null) {
                    throw new IllegalStateException("Mundo não carregado: " + instanceName);
                }

                // Aplica configurações da arena
                applyArenaSettings(world, arena);

                return world;

            } catch (InterruptedException e) {
                throw new IllegalStateException("Erro ao criar instância: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Descarrega uma instância.
     *
     * @param instanceName nome da instância
     */
    public void unloadInstance(@NotNull String instanceName) {
        final World world = Bukkit.getWorld(instanceName);
        if (world != null) {
            // Remove todos os jogadores
            for (final org.bukkit.entity.Player player : world.getPlayers()) {
                player.kick(Component.text("Arena being reset", NamedTextColor.RED));
            }

            Bukkit.unloadWorld(world, true);
            plugin.getLogger().info("Instância descarregada: " + instanceName);
        }
    }

    /**
     * Remove permanentemente uma instância.
     *
     * @param instanceName nome da instância
     */
    public void deleteInstance(@NotNull String instanceName) {
        unloadInstance(instanceName);
        final File folder = getInstanceFolder(instanceName);
        deleteFolder(folder);
    }

    /**
     * Reinicia uma arena (remove instância atual e cria nova).
     *
     * @param arena arena
     * @return CompletableFuture com o novo mundo
     */
    public @NotNull CompletableFuture<World> resetArena(@NotNull Arena arena) {
        final String instanceName = "bw-" + arena.getName();

        // Remove instância antiga
        deleteInstance(instanceName);

        // Cria nova instância
        return createInstance(arena.getName(), arena);
    }

    /**
     * Cria um novo SlimeWorld vazio (VOID).
     *
     * @param name nome do mundo
     * @return SlimeWorld
     */
    public @Nullable SlimeWorld createVoidSlimeWorld(@NotNull String name) {
        if (!isAvailable()) {
            return null;
        }

        final AdvancedSlimePaperAPI api = AdvancedSlimePaperAPI.instance();
        final SlimePropertyMap props = new SlimePropertyMap();
        props.setValue(SlimeProperties.ALLOW_MONSTERS, false);
        props.setValue(SlimeProperties.ALLOW_ANIMALS, false);

        return api.createEmptyWorld(name, false, props, null);
    }

    /**
     * Lista todos os templates disponíveis.
     *
     * @return array com nomes dos templates
     */
    public @NotNull String[] listTemplates() {
        final File[] files = templatesFolder.listFiles(File::isDirectory);
        if (files == null) {
            return new String[0];
        }

        return java.util.Arrays.stream(files)
                .map(File::getName)
                .toArray(String[]::new);
    }

    /**
     * Retorna o diretório de templates.
     *
     * @return diretório
     */
    public @NotNull File getTemplatesFolder() {
        return templatesFolder;
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
     * Retorna o diretório de um template.
     *
     * @param name nome do template
     * @return diretório
     */
    public @NotNull File getTemplateFolder(@NotNull String name) {
        return new File(templatesFolder, name);
    }

    /**
     * Aplica configurações da arena ao mundo.
     *
     * @param world mundo
     * @param arena arena
     */
    @SuppressWarnings("deprecation")
    private void applyArenaSettings(@NotNull World world, @NotNull Arena arena) {
        if (arena.getDifficulty() != null) {
            try {
                world.setDifficulty(org.bukkit.Difficulty.valueOf(arena.getDifficulty().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (arena.getTime() != null) {
            switch (arena.getTime().toUpperCase()) {
                case "DAY" -> world.setTime(1000);
                case "NOON" -> world.setTime(6000);
                case "SUNSET" -> world.setTime(12000);
                case "NIGHT" -> world.setTime(13000);
                case "MIDNIGHT" -> world.setTime(18000);
            }
        }

        if (arena.getWeather() != null) {
            switch (arena.getWeather().toUpperCase()) {
                case "CLEAR" -> {
                    world.setStorm(false);
                    world.setThundering(false);
                }
                case "RAIN" -> {
                    world.setStorm(true);
                    world.setThundering(false);
                }
                case "THUNDER" -> {
                    world.setStorm(true);
                    world.setThundering(true);
                }
            }
        }

        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, arena.isCycleDay());
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, arena.isCycleWeather());
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, arena.isSpawnMobs());
        world.setAnimalSpawnLimit(arena.isSpawnAnimals() ? -1 : 0);
        world.setMonsterSpawnLimit(arena.isSpawnMobs() ? -1 : 0);
    }

    private void copyWorldFolder(@NotNull File source, @NotNull File target) {
        if (!source.isDirectory()) {
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
            plugin.getLogger().severe("Não foi possível criar diretório: " + target);
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
                } catch (Exception e) {
                    plugin.getLogger().warning("Erro ao copiar arquivo " + file.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    private boolean isIgnoredFile(@NotNull String name) {
        return name.equals("uid.dat") || name.equals("session.dat") || name.equals("session.lock");
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
}
