package dev.sebastianjnuwu.bedwars.arena;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.slime.TemplateLoader;

/**
 * Gerencia arenas usando SlimeWorld como sistema principal.
 * FAWE deve ser usado apenas para criação/edição de mapas.
 */
public class ArenaManager {

    private final LangManager lang;
    private final File arenasFolder;
    private final File mapsFolder;
    private final Map<String, Arena> arenas;
    private final Map<String, ArenaInstance> instances;
    private final SlimeManager slimeManager;
    private final TemplateLoader templateLoader;

    /**
     * Cria um novo gerenciador de arenas.
     *
     * @param mapsFolder diretório dos mapas
     * @param slimeManager gerenciador de SlimeWorld
     */
    public ArenaManager(@Nullable File mapsFolder, @Nullable SlimeManager slimeManager) {
        this.lang = org.bukkit.plugin.java.JavaPlugin.getPlugin(dev.sebastianjnuwu.bedwars.BedWarsPlugin.class).getLang();
        this.arenasFolder = new File("arenas");
        this.arenasFolder.mkdirs();
        this.mapsFolder = mapsFolder != null ? mapsFolder : new File("maps");
        this.mapsFolder.mkdirs();
        this.arenas = new ConcurrentHashMap<>();
        this.instances = new ConcurrentHashMap<>();
        this.slimeManager = slimeManager;
        this.templateLoader = new TemplateLoader(slimeManager != null ? slimeManager.getTemplatesFolder() : mapsFolder);
    }

    /**
     * Carrega todas as arenas salvas.
     */
    public void load() {
        final File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
            final Arena arena = loadArenaFromFile(name, file);
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
        arena.setMinPlayers(2);
        arena.setCountdown(3);
        arenas.put(name, arena);
        saveArena(arena);
        return arena;
    }

    /**
     * Salva uma arena no arquivo.
     *
     * @param arena arena
     */
    public void save(@NotNull Arena arena) {
        saveArena(arena);
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

        final File configFile = new File(arenasFolder, name + ".yml");
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

    private void saveArena(@NotNull Arena arena) {
        final File file = new File(arenasFolder, arena.getName() + ".yml");
        final YamlConfiguration config = new YamlConfiguration();

        config.set("enabled", arena.isEnabled());
        setIfNotNull(config, "lobby", serializeLocation(arena.getLobby()));
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        setIfNotNull(config, "arena_spawn", serializeLocation(arena.getArenaSpawn()));
        setIfNotNull(config, "spawn_block", arena.getSpawnBlockData());
        config.set("min_players", arena.getMinPlayers());
        config.set("countdown", arena.getCountdown());

        setIfNotNull(config, "difficulty", arena.getDifficulty());
        setIfNotNull(config, "time", arena.getTime());
        setIfNotNull(config, "weather", arena.getWeather());
        config.set("cycle_day", arena.isCycleDay());
        config.set("cycle_weather", arena.isCycleWeather());
        config.set("spawn_mobs", arena.isSpawnMobs());
        config.set("spawn_animals", arena.isSpawnAnimals());

        config.set("teams", null);
        for (final ArenaTeam team : arena.getTeams()) {
            final String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor());
            setIfNotNull(config, path + ".spawn", serializeLocation(team.getSpawn()));
            setIfNotNull(config, path + ".spawn_block", team.getSpawnBlockData());
            setIfNotNull(config, path + ".bed", serializeLocation(team.getBed()));
            setIfNotNull(config, path + ".bed_facing", team.getBedFacing());
        }

        config.set("generators", null);
        for (int i = 0; i < arena.getGenerators().size(); i++) {
            final ArenaGenerator gen = arena.getGenerators().get(i);
            final String path = "generators." + i;
            config.set(path + ".type", gen.getType());
            setIfNotNull(config, path + ".location", serializeLocation(gen.getLocation()));
            setIfNotNull(config, path + ".team", gen.getTeam());
            setIfNotNull(config, path + ".origin_block", gen.getOriginBlockData());
            setIfNotNull(config, path + ".origin_block_above", gen.getOriginBlockDataAbove());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe(this.lang.raw("log.arena_manager.save_error", arena.getName(), e.getMessage()));
        }
    }

    private @Nullable Arena loadArenaFromFile(@NotNull String name, @NotNull File file) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        final Arena arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setEnabled(config.getBoolean("enabled", false));

        if (config.contains("lobby")) {
            arena.setLobby(parseLocation(config.getString("lobby")));
        }
        if (config.contains("paste")) {
            final String[] parts = config.getString("paste").split(",");
            arena.setPaste(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        if (config.contains("schematic_size")) {
            final String[] parts = config.getString("schematic_size").split(",");
            arena.setSchematicSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        }
        if (config.contains("arena_spawn")) {
            arena.setArenaSpawn(parseLocation(config.getString("arena_spawn")));
        }
        if (config.contains("spawn_block")) {
            arena.setSpawnBlockData(config.getString("spawn_block"));
        }
        arena.setMinPlayers(config.getInt("min_players", 2));
        arena.setCountdown(config.getInt("countdown", 3));

        if (config.contains("difficulty")) {
            arena.setDifficulty(config.getString("difficulty"));
        }
        if (config.contains("time")) {
            arena.setTime(config.getString("time"));
        }
        if (config.contains("weather")) {
            arena.setWeather(config.getString("weather"));
        }
        arena.setCycleDay(config.getBoolean("cycle_day", true));
        arena.setCycleWeather(config.getBoolean("cycle_weather", true));
        arena.setSpawnMobs(config.getBoolean("spawn_mobs", true));
        arena.setSpawnAnimals(config.getBoolean("spawn_animals", true));

        if (config.contains("teams")) {
            for (final String key : config.getConfigurationSection("teams").getKeys(false)) {
                final String path = "teams." + key;
                final ArenaTeam team = new dev.sebastianjnuwu.bedwars.model.ArenaTeam(key, config.getString(path + ".color"));
                if (config.contains(path + ".spawn")) {
                    team.setSpawn(parseLocation(config.getString(path + ".spawn")));
                }
                if (config.contains(path + ".spawn_block")) {
                    team.setSpawnBlockData(config.getString(path + ".spawn_block"));
                }
                if (config.contains(path + ".bed")) {
                    team.setBed(parseLocation(config.getString(path + ".bed")));
                }
                if (config.contains(path + ".bed_facing")) {
                    team.setBedFacing(config.getString(path + ".bed_facing"));
                }
                arena.addTeam(team);
            }
        }

        if (config.contains("generators")) {
            final Set<String> seenLocations = new HashSet<>();
            for (final String key : config.getConfigurationSection("generators").getKeys(false)) {
                final String path = "generators." + key;
                final ArenaGenerator gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(
                        config.getString(path + ".type"),
                        parseLocation(config.getString(path + ".location"))
                );
                if (config.contains(path + ".team")) {
                    gen.setTeam(config.getString(path + ".team"));
                }
                if (config.contains(path + ".origin_block")) {
                    gen.setOriginBlockData(config.getString(path + ".origin_block"));
                }
                if (config.contains(path + ".origin_block_above")) {
                    gen.setOriginBlockDataAbove(config.getString(path + ".origin_block_above"));
                }

                if (gen.getLocation() == null) {
                    continue;
                }

                if (gen.getType().equalsIgnoreCase("forge") && gen.getTeam() != null) {
                    final String dedupeKey = "forge:" + gen.getTeam().toLowerCase();
                    if (!seenLocations.add(dedupeKey)) {
                        Bukkit.getLogger().warning(this.lang.raw("log.arena_manager.forge_duplicate", name, gen.getTeam()));
                        continue;
                    }
                }
                arena.addGenerator(gen);
            }
        }

        return arena;
    }

    private String serializeLocation(@Nullable Location loc) {
        if (loc == null) {
            return null;
        }
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ()
                + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private @Nullable Location parseLocation(@Nullable String str) {
        if (str == null || str.isBlank()) {
            return null;
        }

        final String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }

        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }

        return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                parts.length > 4 ? Float.parseFloat(parts[4]) : 0F,
                parts.length > 5 ? Float.parseFloat(parts[5]) : 0F
        );
    }

    private void setIfNotNull(YamlConfiguration config, String path, Object value) {
        if (value != null) {
            config.set(path, value);
        }
    }
}
