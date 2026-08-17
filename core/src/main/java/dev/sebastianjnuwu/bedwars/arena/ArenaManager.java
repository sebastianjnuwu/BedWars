package dev.sebastianjnuwu.bedwars.arena;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.slime.TemplateLoader;

/**
 * Gerencia arenas usando SlimeWorld como sistema principal.
 * FAWE deve ser usado apenas para criação/edição de mapas.
 */
public class ArenaManager {

    private final LangManager lang;
    private final File mapsFolder;
    private final Map<String, Arena> arenas;
    private final Map<String, ArenaInstance> instances;
    private final SlimeManager slimeManager;
    private final TemplateLoader templateLoader;
    private final ArenaFileStore fileStore;

    /**
     * Cria um novo gerenciador de arenas.
     *
     * @param mapsFolder diretório dos mapas
     * @param slimeManager gerenciador de SlimeWorld
     */
    public ArenaManager(@Nullable File mapsFolder, @Nullable SlimeManager slimeManager) {
        this.lang = JavaPlugin.getPlugin(BedWarsPlugin.class).getLang();
        final File arenasFolder = new File("arenas");
        arenasFolder.mkdirs();
        this.mapsFolder = mapsFolder != null ? mapsFolder : new File("maps");
        this.mapsFolder.mkdirs();
        this.arenas = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.slimeManager = slimeManager;
        this.templateLoader = new TemplateLoader(slimeManager != null ? slimeManager.getTemplatesFolder() : mapsFolder);
        this.fileStore = new ArenaFileStore(lang, arenasFolder);
    }

    /**
     * Carrega todas as arenas salvas.
     */
    public void load() {
        final File[] files = new File("arenas").listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
            final Arena arena = fileStore.load(name, file);
            if (arena != null) {
                arenas.put(name, arena);
            }
        }
    }

    /**
     * Salva um mundo como template Slime.
     * Usado após a criação do mapa com FAWE.
     *
     * @param arena arena
     * @param world mundo original
     * @return CompletableFuture que completa quando o template for salvo
     */
    public @Nullable CompletableFuture<Void> saveTemplate(@NotNull Arena arena, @NotNull World world) {
        if (slimeManager == null) {
            return null;
        }

        return slimeManager.saveTemplate(arena.getName(), world);
    }

    /**
     * Cria uma nova instância de arena para partida.
     *
     * @param arenaName nome da arena
     * @return CompletableFuture com a instância criada
     */
    public @Nullable CompletableFuture<ArenaInstance> createInstance(@NotNull String arenaName) {
        final Arena arena = arenas.get(arenaName);
        if (arena == null) {
            return null;
        }

        if (slimeManager == null) {
            return null;
        }

        return slimeManager.createInstance(arenaName, arena)
                .thenApply(world -> {
                    final ArenaInstance instance = new ArenaInstance(
                            arena,
                            "bw-" + arenaName,
                            arenaName
                    );
                    instance.setWorld(world);
                    instance.setState(ArenaState.READY);
                    instances.put(instance.getInstanceName(), instance);
                    return instance;
                });
    }

    /**
     * Tenta alocar uma arena READY para uso.
     *
     * @return instância pronta ou null
     */
    public @Nullable ArenaInstance allocateInstance() {
        for (final ArenaInstance instance : instances.values()) {
            if (instance.getState() == ArenaState.READY) {
                instance.setState(ArenaState.LOADING);
                return instance;
            }
        }

        return null;
    }

    /**
     * Descarrega e remove uma instância.
     *
     * @param instance instância
     */
    public void releaseInstance(@NotNull ArenaInstance instance) {
        instances.remove(instance.getInstanceName());
        if (slimeManager != null) {
            slimeManager.deleteInstance(instance.getInstanceName());
        }
    }

    /**
     * Reinicia uma arena.
     *
     * @param instance instância
     * @return CompletableFuture com a nova instância
     */
    public @Nullable CompletableFuture<ArenaInstance> resetInstance(@NotNull ArenaInstance instance) {
        if (slimeManager == null) {
            return null;
        }

        return slimeManager.resetArena(instance.getArena())
                .thenApply(world -> {
                    final ArenaInstance newInstance = new ArenaInstance(
                            instance.getArena(),
                            instance.getInstanceName(),
                            instance.getTemplateName()
                    );
                    newInstance.setWorld(world);
                    newInstance.setState(ArenaState.READY);
                    instances.put(newInstance.getInstanceName(), newInstance);
                    return newInstance;
                });
    }

    /**
     * Lista todos os templates disponíveis.
     *
     * @return array com nomes dos templates
     */
    public @NotNull String[] listTemplates() {
        if (slimeManager != null) {
            return slimeManager.listTemplates();
        }
        return templateLoader.listValidTemplates();
    }

    /**
     * Verifica se um template existe.
     *
     * @param name nome do template
     * @return true se existe
     */
    public boolean templateExists(@NotNull String name) {
        if (slimeManager != null) {
            return new File(slimeManager.getTemplatesFolder(), name).exists();
        }
        return templateLoader.templateExists(name);
    }

    /**
     * Retorna uma arena pelo nome.
     *
     * @param name nome
     * @return arena ou null
     */
    public @Nullable Arena get(@NotNull String name) {
        return arenas.get(name);
    }

    /**
     * Retorna todas as arenas.
     *
     * @return mapa de arenas
     */
    public @NotNull Map<String, Arena> getAll() {
        return new HashMap<>(arenas);
    }

    /**
     * Retorna todas as instâncias carregadas.
     *
     * @return mapa de instâncias
     */
    public @NotNull Map<String, ArenaInstance> getInstances() {
        return new HashMap<>(instances);
    }

    /**
     * Cria uma nova arena.
     *
     * @param name nome da arena
     * @return arena criada ou null se já existe
     */
    public @Nullable Arena create(@NotNull String name) {
        if (arenas.containsKey(name)) {
            return null;
        }

        final Arena arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setCountdown(3);
        arenas.put(name, arena);
        fileStore.save(arena);
        return arena;
    }

    /**
     * Salva uma arena no arquivo.
     *
     * @param arena arena
     */
    public void save(@NotNull Arena arena) {
        fileStore.save(arena);
    }

    /**
     * Deleta uma arena.
     *
     * @param name nome da arena
     * @return true se deletado
     */
    public boolean delete(@NotNull String name) {
        final Arena arena = arenas.remove(name);
        if (arena == null) {
            return false;
        }

        final File configFile = new File("arenas", name + ".yml");
        configFile.delete();

        final File mapFile = new File(mapsFolder, name + ".bwmap");
        mapFile.delete();

        if (slimeManager != null) {
            slimeManager.deleteInstance(name);
        } else {
            templateLoader.removeTemplate(name);
        }

        if (slimeManager != null) {
            instances.values().stream()
                    .filter(i -> i.getTemplateName().equals(name))
                    .forEach(i -> slimeManager.deleteInstance(i.getInstanceName()));
        }

        return true;
    }
}
