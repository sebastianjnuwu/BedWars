package dev.sebastianjnuwu.bedwars.manager;

import dev.sebastianjnuwu.bedwars.api.events.ArenaLoadEvent;
import dev.sebastianjnuwu.bedwars.api.events.ArenaSaveEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import dev.sebastianjnuwu.bedwars.world.VoidGenerator;
import dev.sebastianjnuwu.bedwars.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.scheduler.BukkitTask;

/**
 * Gerencia todas as arenas do servidor.
 * Cada arena é salva em um arquivo separado: arenas/&lt;nome&gt;.yml.
 */
public class ArenaManager implements dev.sebastianjnuwu.bedwars.api.ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFolder;
    private final File mapsFolder;
    private final Set<String> dirty;
    private final WorldManager worldManager;

    private BukkitTask saveTask;

    private static final long FLUSH_INTERVAL = 600L; // 30 seconds in ticks

    public ArenaManager(final JavaPlugin plugin, final WorldManager worldManager, final File mapsFolder) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.dirty = new HashSet<>();
        this.worldManager = worldManager;
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
    }

    public WorldManager getWorldManager() {
        return this.worldManager;
    }

    public void startSaveTask() {
        this.saveTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!this.dirty.isEmpty()) {
                this.plugin.getLogger().fine("Salvando " + this.dirty.size() + " arena(s) sujas...");
                this.flush();
            }
        }, FLUSH_INTERVAL, FLUSH_INTERVAL);
    }

    public void stopSaveTask() {
        if (this.saveTask != null) {
            this.saveTask.cancel();
            this.saveTask = null;
        }
    }

    public void load() {
        this.arenas.clear();
        final File[] files = this.arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
            this.arenas.put(name, this.loadArenaFromFile(name, file));
        }
        this.plugin.getLogger().info("Arenas carregadas: " + this.arenas.size());
    }

    /**
     * Recarrega uma arena do disco, atualizando locations quando o mundo estiver carregado.
     */
    public void reload(final String name) {
        final File file = new File(this.arenasFolder, name + ".yml");
        if (!file.exists()) {
            return;
        }
        this.arenas.put(name, this.loadArenaFromFile(name, file));
    }

    /**
     * Garante que o mundo da arena está carregado e recarrega os dados do yml.
     */
    public boolean ensureArenaReady(final Arena arena) {
        if (arena == null) {
            return false;
        }
        final World world = this.ensureWorldLoaded(arena);
        if (world == null) {
            return false;
        }
        this.reload(arena.getName());
        final Arena refreshed = this.get(arena.getName());
        // Se o mundo acabou de ser criado, o arena_spawn pode ser null temporariamente
        // Mas o mundo está pronto, então consideramos a arena pronta
        return refreshed != null;
    }

    public boolean resetArenaMap(final @org.jetbrains.annotations.NotNull String name) {
        final Arena arena = this.get(name);
        if (arena == null) {
            return false;
        }
        final File mapFile = this.getMapFile(name);
        if (mapFile == null) {
            return false;
        }
        final String worldName = "bw_" + name;
        this.worldManager.unloadWorld(worldName);
        this.worldManager.deleteWorld(worldName);
        final WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidGenerator());
        final World world = wc.createWorld();
        if (world == null) {
            return false;
        }
        try {
            final Schematic schematic = Schematic.load(name, mapFile);
            final Location pasteLocation = new Location(
                    world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
            schematic.paste(world, pasteLocation, mapFile);
            world.setSpawnLocation(pasteLocation.getBlockX(), pasteLocation.getBlockY(), pasteLocation.getBlockZ());
            this.applyWorldSettings(world, arena);
            this.reload(name);
            final Arena refreshed = this.get(name);
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
            }
            this.flush(name);
            this.showMarkerBlocks(this.get(name));
            return true;
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao resetar arena " + name + ": " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public void applyWorldSettings(final World world, final Arena arena) {
        if (arena.getDifficulty() != null) {
            try {
                world.setDifficulty(Difficulty.valueOf(arena.getDifficulty().toUpperCase()));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        if (arena.getTime() != null) {
            switch (arena.getTime().toUpperCase()) {
                case "DAY" -> world.setTime(1000);
                case "NOON" -> world.setTime(6000);
                case "SUNSET" -> world.setTime(12000);
                case "NIGHT" -> world.setTime(13000);
                case "MIDNIGHT" -> world.setTime(18000);
                default -> {
                    try { world.setTime(Long.parseLong(arena.getTime())); }
                    catch (final NumberFormatException ignored) {}
                }
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
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, arena.isCycleDay());
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, arena.isCycleWeather());
        world.setGameRule(GameRule.DO_MOB_SPAWNING, arena.isSpawnMobs());
        world.setAnimalSpawnLimit(arena.isSpawnAnimals() ? -1 : 0);
        world.setMonsterSpawnLimit(arena.isSpawnMobs() ? -1 : 0);
    }

    public @Nullable File getMapFile(final String name) {
        // Priorizar formato interno .bwmap
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
        return file.exists() ? file : null;
    }

    public @Nullable World ensureWorldLoaded(final Arena arena) {
        String worldName = arena.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            worldName = "bw_" + arena.getName();
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            this.applyWorldSettings(world, arena);
            return world;
        }
        final File mapFile = this.getMapFile(arena.getName());
        if (mapFile == null) {
            return null;
        }
        final WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidGenerator());
        world = wc.createWorld();
        if (world == null) {
            return null;
        }
        try {
            final Schematic schematic = Schematic.load(arena.getName(), mapFile);
            final Location pasteLocation = new Location(
                    world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
            schematic.paste(world, pasteLocation, mapFile);
            world.setSpawnLocation(pasteLocation.getBlockX(), pasteLocation.getBlockY(), pasteLocation.getBlockZ());
            this.applyWorldSettings(world, arena);
            this.reload(arena.getName());
            final Arena refreshed = this.get(arena.getName());
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
            }
            this.flush(arena.getName());
            return world;
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao carregar mundo da arena " + arena.getName() + ": " + e.getMessage());
            return null;
        }
    }

    public void showMarkerBlocks(final Arena arena) {
        if (arena == null) {
            return;
        }
        if (arena.getArenaSpawn() != null) {
            final var block = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0);
            if (arena.getSpawnBlockData() == null) {
                arena.setSpawnBlockData(block.getBlockData().getAsString());
            }
            block.setType(Material.EMERALD_BLOCK, false);
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                final var block = team.getSpawn().getBlock().getRelative(0, -1, 0);
                if (team.getSpawnBlockData() == null) {
                    team.setSpawnBlockData(block.getBlockData().getAsString());
                }
                block.setType(getTeamConcreteMaterial(team.getColor()), false);
            }
        }
        for (final ArenaGenerator generator : arena.getGenerators()) {
            if (generator.getLocation() == null) {
                continue;
            }
            final var below = generator.getLocation().getBlock().getRelative(0, -1, 0);
            if (generator.getOriginBlockData() == null) {
                generator.setOriginBlockData(below.getBlockData().getAsString());
            }
            final Material marker = this.getGeneratorMarker(generator.getType());
            below.setType(marker, false);
        }
    }

    private Material getTeamConcreteMaterial(final String dyeColor) {
        if (dyeColor == null) return Material.WHITE_CONCRETE;
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO"         -> Material.RED_CONCRETE;
            case "BLUE", "AZUL"            -> Material.BLUE_CONCRETE;
            case "GREEN", "VERDE"          -> Material.GREEN_CONCRETE;
            case "YELLOW", "AMARELO"       -> Material.YELLOW_CONCRETE;
            case "PURPLE", "ROXO"          -> Material.PURPLE_CONCRETE;
            case "PINK", "ROSA"            -> Material.PINK_CONCRETE;
            case "ORANGE", "LARANJA"       -> Material.ORANGE_CONCRETE;
            case "CYAN", "CIANO"           -> Material.CYAN_CONCRETE;
            case "LIME", "VERDE_LIMA"      -> Material.LIME_CONCRETE;
            case "LIGHT_BLUE", "AZUL_CLARO"-> Material.LIGHT_BLUE_CONCRETE;
            case "GRAY", "CINZA"           -> Material.GRAY_CONCRETE;
            case "BLACK", "PRETO"          -> Material.BLACK_CONCRETE;
            default                        -> Material.WHITE_CONCRETE;
        };
    }

    private Material getGeneratorMarker(final String type) {
        return switch (type.toLowerCase()) {
            case "iron"    -> Material.IRON_ORE;
            case "gold"    -> Material.GOLD_ORE;
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "forge"   -> Material.BLAST_FURNACE;
            default        -> Material.SPONGE;
        };
    }

    public void save(final Arena arena) {
        this.dirty.add(arena.getName());
    }

    public void flush() {
        final String[] names = this.dirty.toArray(new String[0]);
        this.dirty.clear();
        for (final String name : names) {
            final Arena arena = this.arenas.get(name);
            if (arena != null) {
                this.flushArena(arena);
            }
        }
    }

    public void flush(final String name) {
        this.dirty.remove(name);
        final Arena arena = this.arenas.get(name);
        if (arena != null) {
            this.flushArena(arena);
        }
    }

    private void flushArena(final Arena arena) {
        final String arenaName = arena.getName();
        final File file = new File(this.arenasFolder, arenaName + ".yml");

        // Verifica permissões da pasta
        if (!this.arenasFolder.exists()) {
            this.plugin.getLogger().severe("Pasta de arenas não existe: " + this.arenasFolder.getAbsolutePath());
            if (!this.arenasFolder.mkdirs()) {
                this.plugin.getLogger().severe("Falha ao criar pasta de arenas (permissões?): "
                        + this.arenasFolder.getAbsolutePath());
                return;
            }
        }
        if (!this.arenasFolder.canWrite()) {
            this.plugin.getLogger().severe("Sem permissão de escrita na pasta de arenas: "
                    + this.arenasFolder.getAbsolutePath());
            return;
        }

        this.plugin.getLogger().fine("Serializando arena '" + arenaName + "' para YML...");

        // Carrega o YML existente como base — o YML é a fonte da verdade
        final YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        try {
            // Campos escalares: sobrescreve só se tiver valor em memória
            // (se null, o valor do YML permanece)
            config.set("enabled", arena.isEnabled());

            if (arena.getLobby() != null)
                config.set("lobby", this.serializeLocation(arena.getLobby()));
            if (arena.getWorldName() != null)
                config.set("world", arena.getWorldName());
            config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
            config.set("schematic_size",
                    arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
            if (arena.getArenaSpawn() != null)
                config.set("arena_spawn", this.serializeLocation(arena.getArenaSpawn()));
            if (arena.getSpawnBlockData() != null)
                config.set("spawn_block", arena.getSpawnBlockData());
            config.set("min_players", arena.getMinPlayers());
            config.set("countdown", arena.getCountdown());

            if (arena.getDifficulty() != null) config.set("difficulty", arena.getDifficulty());
            if (arena.getTime() != null) config.set("time", arena.getTime());
            if (arena.getWeather() != null) config.set("weather", arena.getWeather());
            config.set("cycle_day", arena.isCycleDay());
            config.set("cycle_weather", arena.isCycleWeather());
            config.set("spawn_mobs", arena.isSpawnMobs());
            config.set("spawn_animals", arena.isSpawnAnimals());

            // Teams: remove deletados, adiciona/atualiza da memória
            // Campos null em memória mantêm o valor antigo do YML
            final org.bukkit.configuration.ConfigurationSection teamSection = config.getConfigurationSection("teams");
            if (teamSection != null) {
                for (final String key : teamSection.getKeys(false)) {
                    if (arena.getTeam(key) == null) {
                        config.set("teams." + key, null);
                    }
                }
            }
            for (final ArenaTeam team : arena.getTeams()) {
                final String path = "teams." + team.getName();
                config.set(path + ".color", team.getColor());
                if (team.getSpawn() != null)
                    config.set(path + ".spawn", this.serializeLocation(team.getSpawn()));
                if (team.getSpawnBlockData() != null)
                    config.set(path + ".spawn_block", team.getSpawnBlockData());
                if (team.getBed() != null)
                    config.set(path + ".bed", this.serializeLocation(team.getBed()));
                if (team.getBedFacing() != null)
                    config.set(path + ".bed_facing", team.getBedFacing());
            }

            // Generators: por usarem índices posicionais, precisa limpar e re-escrever
            // Salva valores antigos do YML antes de limpar para usar como fallback
            final java.util.Map<String, java.util.Map<String, String>> oldGens = new java.util.HashMap<>();
            final org.bukkit.configuration.ConfigurationSection genSection = config.getConfigurationSection("generators");
            if (genSection != null) {
                for (final String key : genSection.getKeys(false)) {
                    final java.util.Map<String, String> gd = new java.util.HashMap<>();
                    final String gp = "generators." + key;
                    if (config.contains(gp + ".location")) gd.put("location", config.getString(gp + ".location"));
                    if (config.contains(gp + ".team")) gd.put("team", config.getString(gp + ".team"));
                    if (config.contains(gp + ".origin_block")) gd.put("origin_block", config.getString(gp + ".origin_block"));
                    if (config.contains(gp + ".origin_block_above")) gd.put("origin_block_above", config.getString(gp + ".origin_block_above"));
                    oldGens.put(key, gd);
                }
            }
            config.set("generators", null);
            final List<ArenaGenerator> generators = arena.getGenerators();
            for (int i = 0; i < generators.size(); i++) {
                final ArenaGenerator gen = generators.get(i);
                final String path = "generators." + i;
                config.set(path + ".type", gen.getType());

                final String locStr = gen.getLocation() != null ? this.serializeLocation(gen.getLocation()) : null;
                if (locStr != null) {
                    config.set(path + ".location", locStr);
                } else {
                    final java.util.Map<String, String> oldGen = oldGens.get(Integer.toString(i));
                    if (oldGen != null && oldGen.containsKey("location"))
                        config.set(path + ".location", oldGen.get("location"));
                }

                final String team = gen.getTeam();
                if (team != null) {
                    config.set(path + ".team", team);
                } else {
                    final java.util.Map<String, String> oldGen = oldGens.get(Integer.toString(i));
                    if (oldGen != null && oldGen.containsKey("team"))
                        config.set(path + ".team", oldGen.get("team"));
                }

                final String obd = gen.getOriginBlockData();
                if (obd != null) {
                    config.set(path + ".origin_block", obd);
                } else {
                    final java.util.Map<String, String> oldGen = oldGens.get(Integer.toString(i));
                    if (oldGen != null && oldGen.containsKey("origin_block"))
                        config.set(path + ".origin_block", oldGen.get("origin_block"));
                }

                final String obda = gen.getOriginBlockDataAbove();
                if (obda != null) {
                    config.set(path + ".origin_block_above", obda);
                } else {
                    final java.util.Map<String, String> oldGen = oldGens.get(Integer.toString(i));
                    if (oldGen != null && oldGen.containsKey("origin_block_above"))
                        config.set(path + ".origin_block_above", oldGen.get("origin_block_above"));
                }
            }
        } catch (final Exception e) {
            this.plugin.getLogger().severe("Erro durante serialização da arena '" + arenaName + "': "
                    + e.getMessage());
            return;
        }

        // Evento pré-save (antes de escrever o arquivo)
        final World saveWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        final ArenaSaveEvent saveEvent = new ArenaSaveEvent(arena, saveWorld);
        Bukkit.getPluginManager().callEvent(saveEvent);
        if (saveEvent.isCancelled()) {
            this.plugin.getLogger().fine("Save da arena '" + arenaName + "' cancelado por evento externo.");
            return;
        }

        // Escrita do arquivo
        this.plugin.getLogger().fine("Escrevendo arquivo YML da arena '" + arenaName + "' em: "
                + file.getAbsolutePath());
        try {
            config.save(file);
            this.plugin.getLogger().fine("Arena '" + arenaName + "' salva com sucesso em "
                    + file.getAbsolutePath());
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao salvar arquivo da arena '" + arenaName + "' em "
                    + file.getAbsolutePath() + ": " + e.getMessage());
            this.plugin.getLogger().severe("Verifique permissões de escrita e espaço em disco.");
        }
    }

    public Arena create(final String name) {
        if (this.arenas.containsKey(name)) {
            return null;
        }
        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setMinPlayers(2);
        arena.setCountdown(3);
        this.arenas.put(name, arena);
        this.save(arena);
        return arena;
    }

    public boolean delete(final String name) {
        final Arena arena = this.arenas.remove(name);
        if (arena == null) {
            return false;
        }

        final File configFile = new File(this.arenasFolder, name + ".yml");
        if (configFile.exists()) {
            configFile.delete();
        }

        final File mapFile = new File(this.mapsFolder, name + ".bwmap");
        if (mapFile.exists()) {
            mapFile.delete();
        }

        this.worldManager.deleteWorld("bw_" + name);
        this.worldManager.deleteWorld("bw_" + name + "_edit");

        final File template = this.worldManager.getTemplateFolder(name);
        if (template.exists()) {
            deleteDirectory(template);
        }

        return true;
    }

    private void deleteDirectory(final File path) {
        if (!path.exists()) return;
        final File[] files = path.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

    public Arena get(final String name) {
        return this.arenas.get(name);
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

    private Arena loadArenaFromFile(final String name, final File file) {
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setEnabled(config.getBoolean("enabled", false));

        if (config.contains("lobby")) {
            arena.setLobby(this.parseLocation(config.getString("lobby")));
        }
        if (config.contains("world")) {
            arena.setWorldName(config.getString("world"));
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
            arena.setArenaSpawn(this.parseLocation(config.getString("arena_spawn")));
        }
        if (config.contains("spawn_block")) {
            arena.setSpawnBlockData(config.getString("spawn_block"));
        }
        arena.setMinPlayers(config.getInt("min_players", 2));
        arena.setCountdown(config.getInt("countdown", 3));
        if (config.contains("difficulty")) arena.setDifficulty(config.getString("difficulty"));
        if (config.contains("time")) arena.setTime(config.getString("time"));
        if (config.contains("weather")) arena.setWeather(config.getString("weather"));
        arena.setCycleDay(config.getBoolean("cycle_day", true));
        arena.setCycleWeather(config.getBoolean("cycle_weather", true));
        arena.setSpawnMobs(config.getBoolean("spawn_mobs", true));
        arena.setSpawnAnimals(config.getBoolean("spawn_animals", true));
        if (config.contains("teams")) {
            for (final String key : config.getConfigurationSection("teams").getKeys(false)) {
                final String path = "teams." + key;
                final var team = new dev.sebastianjnuwu.bedwars.model.ArenaTeam(key, config.getString(path + ".color"));
                if (config.contains(path + ".spawn")) {
                    team.setSpawn(this.parseLocation(config.getString(path + ".spawn")));
                }
                if (config.contains(path + ".spawn_block")) {
                    team.setSpawnBlockData(config.getString(path + ".spawn_block"));
                }
                if (config.contains(path + ".bed")) {
                    team.setBed(this.parseLocation(config.getString(path + ".bed")));
                }
                if (config.contains(path + ".bed_facing")) {
                    team.setBedFacing(config.getString(path + ".bed_facing"));
                }
                arena.addTeam(team);
            }
        }
        if (config.contains("generators")) {
            final java.util.Set<String> seenLocations = new java.util.HashSet<>();
            for (final String key : config.getConfigurationSection("generators").getKeys(false)) {
                final String path = "generators." + key;
                final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(
                        config.getString(path + ".type"),
                        this.parseLocation(config.getString(path + ".location"))
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
                // Deduplicate: skip forge generators that share the same team as one already loaded
                if (gen.getType().equalsIgnoreCase("forge") && gen.getTeam() != null) {
                    final String dedupeKey = "forge:" + gen.getTeam().toLowerCase();
                    if (!seenLocations.add(dedupeKey)) {
                        this.plugin.getLogger().warning(
                                "Arena " + name + ": forge duplicado para o time '" + gen.getTeam() + "' ignorado ao carregar.");
                        continue;
                    }
                }
                arena.addGenerator(gen);
            }
        }

        final World loadWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        Bukkit.getPluginManager().callEvent(new ArenaLoadEvent(arena, loadWorld));
        return arena;
    }

    private String serializeLocation(final Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ()
                + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private @Nullable Location parseLocation(final String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        final String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }
        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            // Se o mundo não está carregado, retorna null temporariamente
            // Será recarregado quando o mundo estiver disponível
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
}
