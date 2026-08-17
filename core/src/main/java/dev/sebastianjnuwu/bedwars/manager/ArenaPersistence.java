package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.events.ArenaLoadEvent;
import dev.sebastianjnuwu.bedwars.api.events.ArenaSaveEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Responsável pela persistência das arenas em disco: leitura do yml
 * ({@code arenas/<nome>.yml}), escrita, recarga e conversão de localizações.
 * Preserva seções do disco quando o mundo da arena ainda não está carregado.
 * <p>
 * A leitura/escrita das seções (times, geradores, NPCs, configs) fica em
 * {@link ArenaYamlMapper}.
 * </p>
 */
public final class ArenaPersistence {

    private final ArenaManager manager;
    private final ArenaYamlMapper mapper;

    /**
     * Cria o gerenciador de persistência de arenas.
     *
     * @param manager arena manager que será alcançado por este gerenciador (não nulo)
     */
    public ArenaPersistence(final ArenaManager manager) {
        this.manager = manager;
        this.mapper = new ArenaYamlMapper();
    }

    /**
     * Grava todas as arenas em memória em seus arquivos.
     */
    public void flushAll() {
        for (final Arena arena : this.manager.arenas.values()) {
            this.writeArenaToFile(arena);
        }
    }

    /**
     * Grava uma arena específica em seu arquivo, se estiver em memória.
     *
     * @param name nome da arena
     */
    public void flush(final String name) {
        final Arena arena = this.manager.arenas.get(name);
        if (arena != null) {
            this.writeArenaToFile(arena);
        }
    }

    /**
     * Grava a arena informada em disco.
     *
     * @param arena arena a ser salva (não nula)
     */
    public void save(final Arena arena) {
        this.writeArenaToFile(arena);
    }

    /**
     * Localiza o arquivo yml de uma arena (por nome exato ou case-insensitive).
     *
     * @param name nome da arena
     * @return arquivo da arena ou {@code null}
     */
    public @Nullable File findArenaFile(final String name) {
        final File direct = new File(this.manager.arenasFolder, name + ".yml");
        if (direct.exists()) {
            return direct;
        }
        final File[] files = this.manager.arenasFolder.listFiles((dir, fileName) -> fileName.endsWith(".yml"));
        if (files == null) {
            return null;
        }
        for (final File file : files) {
            if (file.getName().replace(".yml", "").equalsIgnoreCase(name)) {
                return file;
            }
        }
        return null;
    }

    /**
     * Carrega uma arena do disco sem rebasear localizações.
     *
     * @param name nome da arena
     * @param file arquivo yml
     * @return arena carregada (não nula)
     */
    public Arena load(final String name, final File file) {
        return this.load(name, file, null);
    }

    /**
     * Carrega uma arena do disco, rebaseando as localizações para o mundo alvo.
     * Quando {@code targetWorld} é nulo, dispara o {@link ArenaLoadEvent} e
     * armazena o yml lido como referência do disco.
     *
     * @param name        nome da arena
     * @param file        arquivo yml
     * @param targetWorld mundo de partida para rebasear localizações ou {@code null}
     * @return arena carregada (não nula)
     */
    public Arena load(final String name, final File file, final @Nullable World targetWorld) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setEnabled(config.getBoolean("enabled", false));

        if (config.contains("lobby")) {
            arena.setLobby(this.mapper.parseLocationFor(config.getString("lobby"), targetWorld));
        }
        if (config.contains("world")) {
            arena.setWorldName(config.getString("world"));
        }
        if (config.contains("map")) {
            arena.setMapName(config.getString("map"));
        }
        if (config.contains("paste")) {
            final String[] parts = config.getString("paste").split(",");
            arena.setPaste(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        }
        if (config.contains("schematic_size")) {
            final String[] parts = config.getString("schematic_size").split(",");
            arena.setSchematicSize(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
            );
        }
        if (config.contains("arena_spawn")) {
            arena.setArenaSpawn(this.mapper.parseLocationFor(config.getString("arena_spawn"), targetWorld));
        }
        if (config.contains("spawn_block")) {
            arena.setSpawnBlockData(config.getString("spawn_block"));
        }
        arena.setMinPlayersPerTeam(config.getInt("teams.min-players", 1));
        arena.setMaxPlayersPerTeam(config.getInt("teams.max-players", 0));
        arena.setMinTeamsToStart(config.getInt("teams.min-teams", 2));
        arena.setCountdown(config.getInt("countdown", 3));
        arena.setRespawnDelay(config.getInt("respawn-delay", 3));
        arena.setTimeLimit(config.getInt("time-limit", 0));
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

        this.mapper.readSpawnItems(config, arena);
        this.mapper.readTeams(config, arena, targetWorld);
        this.mapper.readGenerators(config, arena, targetWorld);

        if (config.contains("shop")) {
            arena.setShop(config.getString("shop"));
        }

        if (config.contains("enable-cmd")) {
            arena.setEnabledCommands(this.mapper.parseEnabledCommands(config.get("enable-cmd")));
        }

        this.mapper.readShopNpcs(config, arena, targetWorld);
        this.mapper.readGeneratorConfigs(config, arena);
        this.mapper.readLevelTimes(config, arena);

        if (targetWorld == null) {
            final World loadWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
            Bukkit.getPluginManager().callEvent(new ArenaLoadEvent(arena, loadWorld));
            this.manager.diskConfigs.put(name, config);
        }
        return arena;
    }

    private void writeArenaToFile(final Arena arena) {
        final String arenaName = arena.getName();
        final File file = new File(this.manager.arenasFolder, arenaName + ".yml");

        if (!this.manager.arenasFolder.exists()) {
            this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.folder_not_exist", this.manager.arenasFolder.getAbsolutePath()));
            if (!this.manager.arenasFolder.mkdirs()) {
                this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.folder_create_fail", this.manager.arenasFolder.getAbsolutePath()));
                return;
            }
        }
        if (!this.manager.arenasFolder.canWrite()) {
            this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.folder_no_permission", this.manager.arenasFolder.getAbsolutePath()));
            return;
        }

        final YamlConfiguration config = new YamlConfiguration();
        final YamlConfiguration disk = this.manager.diskConfigs.get(arenaName);

        config.set("enabled", arena.isEnabled());
        this.mapper.writeLocation(config, disk, "lobby", arena.getLobby());
        if (arena.getWorldName() != null) {
            config.set("world", arena.getWorldName());
        }
        if (arena.getMapName() != null) {
            config.set("map", arena.getMapName());
        }
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        this.mapper.writeLocation(config, disk, "arena_spawn", arena.getArenaSpawn());
        if (arena.getSpawnBlockData() != null) {
            config.set("spawn_block", arena.getSpawnBlockData());
        }
        config.set("teams.min-players", arena.getMinPlayersPerTeam());
        config.set("teams.max-players", arena.getMaxPlayersPerTeam());
        config.set("teams.min-teams", arena.getMinTeamsToStart());
        config.set("countdown", arena.getCountdown());
        config.set("respawn-delay", arena.getRespawnDelay());
        config.set("time-limit", arena.getTimeLimit());

        if (arena.getDifficulty() != null) {
            config.set("difficulty", arena.getDifficulty());
        }
        if (arena.getTime() != null) {
            config.set("time", arena.getTime());
        }
        if (arena.getWeather() != null) {
            config.set("weather", arena.getWeather());
        }
        config.set("cycle_day", arena.isCycleDay());
        config.set("cycle_weather", arena.isCycleWeather());
        config.set("spawn_mobs", arena.isSpawnMobs());
        config.set("spawn_animals", arena.isSpawnAnimals());

        this.mapper.writeSpawnItems(config, arena);

        if (arena.getShop() != null) {
            config.set("shop", arena.getShop());
        }

        if (arena.getEnabledCommands() != null && !arena.getEnabledCommands().isEmpty()) {
            config.set("enable-cmd", arena.getEnabledCommands());
        } else {
            config.set("enable-cmd", null);
        }

        this.mapper.writeShopNpcs(config, disk, arena);
        this.mapper.writeGeneratorConfigs(config, arena);
        this.mapper.writeLevelTimes(config, arena);
        this.mapper.writeTeams(config, disk, arena);
        this.mapper.writeGenerators(config, disk, arena);

        // Evento pré-save
        final World saveWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        final ArenaSaveEvent saveEvent = new ArenaSaveEvent(arena, saveWorld);
        Bukkit.getPluginManager().callEvent(saveEvent);
        if (saveEvent.isCancelled()) {
            return;
        }

        try {
            config.save(file);
            this.manager.diskConfigs.put(arenaName, config);
        } catch (final IOException e) {
            this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.save_arena_error", arenaName, e.getMessage()));
        }
    }
}