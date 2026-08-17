package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.Saveable;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.world.WorldProvider;

/**
 * Gerencia todas as arenas do servidor.
 * Cada arena é salva em um arquivo separado: arenas/<nome>.yml.
 * <p>
 * A implementação é delegada a dois gerenciadores internos: {@link ArenaPersistence}
 * (leitura/escrita do yml) e {@link ArenaWorldService} (mundos, instâncias e
 * restauração de blocos especiais).
 * </p>
 */
public class ArenaManager implements dev.sebastianjnuwu.bedwars.api.ArenaManager, Saveable {

    final JavaPlugin plugin;
    final Map<String, Arena> arenas;
    final File arenasFolder;
    final File mapsFolder;
    final WorldProvider worldProvider;
    final LangManager lang;
    final Map<String, YamlConfiguration> diskConfigs;
    final Set<String> cleanWorlds;
    final Map<String, Integer> instanceCounters;
    private final ArenaPersistence persistence;
    private final ArenaWorldService worldService;

    public ArenaManager(final JavaPlugin plugin, final WorldProvider worldProvider, final File mapsFolder) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.arenas = new HashMap<>();
        this.diskConfigs = new HashMap<>();
        this.worldProvider = worldProvider;
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
        this.cleanWorlds = new HashSet<>();
        this.instanceCounters = new HashMap<>();
        this.persistence = new ArenaPersistence(this);
        this.worldService = new ArenaWorldService(this);
    }

    public boolean isWorldClean(final String worldName) {
        return this.cleanWorlds.contains(worldName);
    }

    public void markWorldClean(final String worldName) {
        this.cleanWorlds.add(worldName);
    }

    public void markWorldDirty(final String worldName) {
        this.cleanWorlds.remove(worldName);
    }

    public boolean deleteWorld(final String worldName) {
        return this.worldProvider.deleteWorld(worldName);
    }

    public void load() {
        this.arenas.clear();
        final File[] files = this.arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
            this.arenas.put(name, this.persistence.load(name, file));
        }
        this.plugin.getLogger().info(this.lang.raw("log.arena_manager.loaded", String.valueOf(this.arenas.size())));
    }

    /**
     * Recarrega uma arena do disco, atualizando locations quando o mundo estiver carregado.
     */
    public void reload(final String name) {
        final File file = this.persistence.findArenaFile(name);
        if (file == null) {
            return;
        }
        final String canonical = file.getName().replace(".yml", "");
        this.arenas.put(canonical, this.persistence.load(canonical, file));
    }

    /**
     * Garante que o mundo da arena está carregado e recarrega os dados do yml.
     */
    public boolean ensureArenaReady(final Arena arena) {
        return this.worldService.ensureArenaReady(arena);
    }

    public boolean resetArenaMap(final @org.jetbrains.annotations.NotNull String name) {
        return this.worldService.resetArenaMap(name);
    }

    /**
     * Cria uma nova instância de partida para uma arena, clonando a configuração
     * e construindo um mundo de partida dedicado ({@code bw_<arena>_<id>}).
     *
     * @param arenaName nome da arena (não nulo)
     * @return a arena clonada com o mundo de partida pronto, ou {@code null} se falhar
     */
    public @Nullable Arena createInstance(final String arenaName) {
        return this.worldService.createInstance(arenaName);
    }

    /**
     * Cria uma instância de partida de forma assíncrona, delegando a construção
     * do mundo ao backend ativo e invocando o callback na main thread.
     *
     * @param arenaName nome da arena (não nulo)
     * @param callback  consumidor chamado na main thread com a instância pronta (não nulo)
     */
    public void createInstanceAsync(final String arenaName, final @org.jetbrains.annotations.NotNull java.util.function.Consumer<Arena> callback) {
        this.worldService.createInstanceAsync(arenaName, callback);
    }

    /**
     * Remove o mundo de uma instância de partida (unload + exclusão do disco).
     *
     * @param worldName nome do mundo de partida
     */
    public void deleteInstanceWorld(final String worldName) {
        this.worldService.deleteInstanceWorld(worldName);
    }

    public void applyWorldSettings(final World world, final Arena arena) {
        this.worldService.applyWorldSettings(world, arena);
    }

    public @Nullable File getMapFile(final String name) {
        return this.worldService.getMapFile(name);
    }

    /**
     * Resolve o arquivo de mapa usado por uma arena, considerando o mapa
     * compartilhado ({@link Arena#getMapName()}) quando configurado.
     *
     * @param arena arena cujo mapa deve ser resolvido (não nula)
     * @return arquivo do schematic, ou {@code null} se não encontrado
     */
    public @Nullable File getMapFile(final Arena arena) {
        return this.worldService.getMapFile(arena);
    }

    /**
     * Garante que o mundo da arena está carregado e com as referências atualizadas.
     *
     * @param arena a arena cujo mundo deve estar pronto (não nula)
     * @return o mundo carregado/pronto, ou {@code null} se não foi possível
     */
    public @Nullable World ensureWorldLoaded(final Arena arena) {
        return this.worldService.ensureWorldLoaded(arena);
    }

    /**
     * Restaura as camas das equipes após a reconstrução do mundo.
     *
     * @param world mundo onde as camas serão colocadas (não nulo)
     * @param arena arena com as configurações das camas (não nula)
     */
    public void restoreBeds(final World world, final Arena arena) {
        this.worldService.restoreBeds(world, arena);
    }

    public void showMarkerBlocks(final Arena arena) {
        this.worldService.showMarkerBlocks(arena);
    }

    public void save(final Arena arena) {
        this.persistence.save(arena);
    }

    @Override
    public void save() {
        this.flush();
    }

    public void flush() {
        this.persistence.flushAll();
    }

    public void flush(final String name) {
        this.persistence.flush(name);
    }

    public Arena create(final String name) {
        if (this.arenas.containsKey(name)) {
            return null;
        }
        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setMinPlayersPerTeam(1);
        arena.setMaxPlayersPerTeam(0);
        arena.setMinTeamsToStart(2);
        arena.setTimeLimit(0);
        arena.setCycleDay(true);
        arena.setCycleWeather(true);
        arena.setSpawnMobs(true);
        arena.setSpawnAnimals(true);
        arena.setEnabled(false);
        arena.setShop("default");
        arena.setCountdown(3);
        arena.setRespawnDelay(3);
        // Default generator configs
        Map<String, GeneratorConfig> genConfigs = new java.util.HashMap<>();
        genConfigs.put("iron", new GeneratorConfig(Material.IRON_INGOT, java.util.Map.of(1, 40L, 2, 35L, 3, 30L, 4, 25L, 5, 20L)));
        genConfigs.put("gold", new GeneratorConfig(Material.GOLD_INGOT, java.util.Map.of(1, 120L, 2, 100L, 3, 80L, 4, 60L, 5, 40L)));
        genConfigs.put("diamond", new GeneratorConfig(Material.DIAMOND, java.util.Map.of(1, 600L, 2, 500L, 3, 400L, 4, 300L, 5, 200L)));
        genConfigs.put("emerald", new GeneratorConfig(Material.EMERALD, java.util.Map.of(1, 1200L, 2, 1000L, 3, 800L, 4, 600L, 5, 400L)));
        arena.setGeneratorConfigs(genConfigs);
        // Default level times (minutos -> nivel)
        arena.setLevelTimes(java.util.Map.of(0, 1, 5, 2, 10, 3, 15, 4, 20, 5));
        this.arenas.put(name, arena);
        this.save(arena);
        return arena;
    }

    public boolean delete(final String name) {
        final Arena arena = this.arenas.remove(name);
        if (arena == null) {
            return false;
        }
        this.diskConfigs.remove(name);

        final File configFile = new File(this.arenasFolder, name + ".yml");
        if (configFile.exists()) {
            configFile.delete();
        }

        final String mapName = arena.getMapName();
        if (mapName == null || mapName.isBlank() || mapName.equals(name)) {
            final File mapFile = this.worldService.getMapFile(name);
            if (mapFile != null) {
                mapFile.delete();
            }
        }

        this.worldProvider.deleteWorld("bw_" + name);
        this.worldProvider.deleteWorld("bw_" + name + "_edit");
        this.markWorldDirty("bw_" + name);
        this.markWorldDirty("bw_" + name + "_edit");

        final File template = this.worldProvider.getTemplateFolder(name);
        if (template != null && template.exists()) {
            this.worldService.deleteDirectory(template);
        }

        return true;
    }

    public Arena get(final String name) {
        if (name == null) {
            return null;
        }
        final Arena direct = this.arenas.get(name);
        if (direct != null) {
            return direct;
        }
        for (final Map.Entry<String, Arena> entry : this.arenas.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(this.arenas.keySet());
    }

    public Collection<Arena> getAll() {
        return Collections.unmodifiableCollection(this.arenas.values());
    }

    public File getMapsFolder() {
        return this.mapsFolder;
    }

    /**
     * Retorna o gerenciador de persistência de arenas.
     *
     * @return gerenciador de persistência (não nulo)
     */
    ArenaPersistence persistence() {
        return this.persistence;
    }
}