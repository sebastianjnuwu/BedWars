package dev.sebastianjnuwu.bedwars.manager;

import dev.sebastianjnuwu.bedwars.api.events.ArenaLoadEvent;
import dev.sebastianjnuwu.bedwars.api.events.ArenaSaveEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Gerencia todas as arenas do servidor.
 * Cada arena é salva em um arquivo separado: arenas/&lt;nome&gt;.yml.
 */
public class ArenaManager implements dev.sebastianjnuwu.bedwars.api.ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFolder;
    private final File mapsFolder;
    private final WorldManager worldManager;
    private final LangManager lang;

    public ArenaManager(final JavaPlugin plugin, final WorldManager worldManager, final File mapsFolder) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.arenas = new HashMap<>();
        this.worldManager = worldManager;
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
    }

    public WorldManager getWorldManager() {
        return this.worldManager;
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
        this.plugin.getLogger().info(this.lang.raw("log.arena_manager.loaded", String.valueOf(this.arenas.size())));
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

    private void updateWorldReferences(final Arena arena, final World newWorld) {
        if (arena.getArenaSpawn() != null) {
            final Location old = arena.getArenaSpawn();
            arena.setArenaSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        if (arena.getLobby() != null) {
            final Location old = arena.getLobby();
            arena.setLobby(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                final Location old = team.getSpawn();
                team.setSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
            if (team.getBed() != null) {
                final Location old = team.getBed();
                team.setBed(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        for (final ArenaGenerator gen : arena.getGenerators()) {
            if (gen.getLocation() != null) {
                final Location old = gen.getLocation();
                gen.setLocation(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        if (arena.getShopNpcLocations() != null) {
            final List<Location> updated = new ArrayList<>();
            for (final Location loc : arena.getShopNpcLocations()) {
                updated.add(new Location(newWorld, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
            }
            arena.setShopNpcLocations(updated);
        }
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
            arena.setWorldName(worldName);
            this.updateWorldReferences(arena, world);
            this.flush(arena.getName());
            this.showMarkerBlocks(this.get(name));
            return true;
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.reset_error", name, e.getMessage()));
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
                this.updateWorldReferences(refreshed, world);
            }
            this.flush(arena.getName());
            return world;
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.load_error", arena.getName(), e.getMessage()));
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
        this.writeArenaToFile(arena);
    }

    public void flush() {
        for (final Arena arena : this.arenas.values()) {
            this.writeArenaToFile(arena);
        }
    }

    public void flush(final String name) {
        final Arena arena = this.arenas.get(name);
        if (arena != null) {
            this.writeArenaToFile(arena);
        }
    }

    private void writeArenaToFile(final Arena arena) {
        final String arenaName = arena.getName();
        final File file = new File(this.arenasFolder, arenaName + ".yml");

        if (!this.arenasFolder.exists()) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.folder_not_exist", this.arenasFolder.getAbsolutePath()));
            if (!this.arenasFolder.mkdirs()) {
                this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.folder_create_fail", this.arenasFolder.getAbsolutePath()));
                return;
            }
        }
        if (!this.arenasFolder.canWrite()) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.folder_no_permission", this.arenasFolder.getAbsolutePath()));
            return;
        }

        final YamlConfiguration config = new YamlConfiguration();

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

        if (arena.getShop() != null) config.set("shop", arena.getShop());

        // Shop NPCs
        List<Location> npcLocs = arena.getShopNpcLocations();
        if (npcLocs != null) {
            for (int i = 0; i < npcLocs.size(); i++) {
                config.set("shop_npcs." + i + ".location", this.serializeLocation(npcLocs.get(i)));
            }
            if (arena.getShopNpcSkin() != null) {
                config.set("shop_npcs.skin", arena.getShopNpcSkin());
            }
        }

        // Generator configs
        for (var entry : arena.getGeneratorConfigs().entrySet()) {
            String type = entry.getKey();
            GeneratorConfig gc = entry.getValue();
            config.set("generator_config." + type + ".material", gc.material().name());
            config.set("generator_config." + type + ".interval", gc.interval());
        }

        // Forge levels
        config.set("forge.max-level", arena.getForgeMaxLevel());
        for (ForgeLevel fl : arena.getForgeLevels()) {
            String levelPath = "forge.levels." + fl.level();
            for (var entry : fl.intervals().entrySet()) {
                config.set(levelPath + "." + entry.getKey().name().toLowerCase() + ".interval", entry.getValue());
            }
        }

        // Teams
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

        // Generators — usa UUID como chave, ignora location null
        for (final ArenaGenerator gen : arena.getGenerators()) {
            if (gen.getLocation() == null) {
                continue;
            }
            final String path = "generators." + gen.getUniqueId().toString();
            config.set(path + ".type", gen.getType());
            config.set(path + ".location", this.serializeLocation(gen.getLocation()));
            if (gen.getTeam() != null)
                config.set(path + ".team", gen.getTeam());
            if (gen.getOriginBlockData() != null)
                config.set(path + ".origin_block", gen.getOriginBlockData());
            if (gen.getOriginBlockDataAbove() != null)
                config.set(path + ".origin_block_above", gen.getOriginBlockDataAbove());
        }

        // Evento pré-save
        final World saveWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
        final ArenaSaveEvent saveEvent = new ArenaSaveEvent(arena, saveWorld);
        Bukkit.getPluginManager().callEvent(saveEvent);
        if (saveEvent.isCancelled()) {
            return;
        }

        try {
            config.save(file);
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.save_arena_error", arenaName, e.getMessage()));
        }
    }

    public Arena create(final String name) {
        if (this.arenas.containsKey(name)) {
            return null;
        }
        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setMinPlayers(2);
        arena.setCountdown(3);
        // Default generator configs
        Map<String, GeneratorConfig> genConfigs = new java.util.HashMap<>();
        genConfigs.put("iron", new GeneratorConfig(Material.IRON_INGOT, 40));
        genConfigs.put("gold", new GeneratorConfig(Material.GOLD_INGOT, 120));
        genConfigs.put("diamond", new GeneratorConfig(Material.DIAMOND, 600));
        genConfigs.put("emerald", new GeneratorConfig(Material.EMERALD, 1200));
        arena.setGeneratorConfigs(genConfigs);
        // Default forge levels
        arena.setForgeMaxLevel(10);
        List<ForgeLevel> forgeLevels = new java.util.ArrayList<>();
        forgeLevels.add(new ForgeLevel(1, Map.of(Material.IRON_INGOT, 20L)));
        forgeLevels.add(new ForgeLevel(2, Map.of(Material.IRON_INGOT, 18L, Material.GOLD_INGOT, 100L)));
        forgeLevels.add(new ForgeLevel(3, Map.of(Material.IRON_INGOT, 16L, Material.GOLD_INGOT, 90L)));
        forgeLevels.add(new ForgeLevel(4, Map.of(Material.IRON_INGOT, 14L, Material.GOLD_INGOT, 80L, Material.DIAMOND, 1200L)));
        forgeLevels.add(new ForgeLevel(5, Map.of(Material.IRON_INGOT, 12L, Material.GOLD_INGOT, 70L, Material.DIAMOND, 1000L)));
        forgeLevels.add(new ForgeLevel(6, Map.of(Material.IRON_INGOT, 10L, Material.GOLD_INGOT, 60L, Material.DIAMOND, 800L, Material.EMERALD, 2400L)));
        forgeLevels.add(new ForgeLevel(7, Map.of(Material.IRON_INGOT, 8L, Material.GOLD_INGOT, 50L, Material.DIAMOND, 700L, Material.EMERALD, 2000L)));
        forgeLevels.add(new ForgeLevel(8, Map.of(Material.IRON_INGOT, 6L, Material.GOLD_INGOT, 40L, Material.DIAMOND, 600L, Material.EMERALD, 1600L)));
        forgeLevels.add(new ForgeLevel(9, Map.of(Material.IRON_INGOT, 5L, Material.GOLD_INGOT, 30L, Material.DIAMOND, 500L, Material.EMERALD, 1400L)));
        forgeLevels.add(new ForgeLevel(10, Map.of(Material.IRON_INGOT, 4L, Material.GOLD_INGOT, 20L, Material.DIAMOND, 400L, Material.EMERALD, 1200L)));
        arena.setForgeLevels(forgeLevels);
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
            for (final String key : config.getConfigurationSection("generators").getKeys(false)) {
                final String path = "generators." + key;
                final Location loc = this.parseLocation(config.getString(path + ".location"));
                // Skip entries with null location (corrupted)
                if (loc == null) {
                    continue;
                }
                final String type = config.getString(path + ".type");
                if (type == null) continue;

                // Determine UUID: use the YAML key if valid UUID, otherwise generate new
                UUID genUuid;
                try {
                    genUuid = UUID.fromString(key);
                } catch (final IllegalArgumentException e) {
                    genUuid = UUID.randomUUID();
                }

                final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(genUuid, type, loc);
                if (config.contains(path + ".team")) {
                    gen.setTeam(config.getString(path + ".team"));
                }
                if (config.contains(path + ".origin_block")) {
                    gen.setOriginBlockData(config.getString(path + ".origin_block"));
                }
                if (config.contains(path + ".origin_block_above")) {
                    gen.setOriginBlockDataAbove(config.getString(path + ".origin_block_above"));
                }
                arena.addGenerator(gen);
            }
        }

        if (config.contains("shop")) {
            arena.setShop(config.getString("shop"));
        }

        if (config.contains("shop_npcs")) {
            List<Location> npcLocs = new java.util.ArrayList<>();
            ConfigurationSection npcSection = config.getConfigurationSection("shop_npcs");
            if (npcSection != null) {
                for (String key : npcSection.getKeys(false)) {
                    if (key.equals("skin")) continue;
                    Location loc = this.parseLocation(config.getString("shop_npcs." + key + ".location"));
                    if (loc != null) npcLocs.add(loc);
                }
            }
            if (!npcLocs.isEmpty()) arena.setShopNpcLocations(npcLocs);
            String skin = config.getString("shop_npcs.skin");
            if (skin != null) arena.setShopNpcSkin(skin);
        }

        if (config.contains("generator_config")) {
            Map<String, GeneratorConfig> genConfigs = new java.util.HashMap<>();
            for (String type : config.getConfigurationSection("generator_config").getKeys(false)) {
                String path = "generator_config." + type;
                String matName = config.getString(path + ".material");
                Material mat = matName != null ? Material.matchMaterial(matName) : null;
                long interval = config.getLong(path + ".interval", 0L);
                if (mat != null && interval > 0L) {
                    genConfigs.put(type, new GeneratorConfig(mat, interval));
                }
            }
            if (!genConfigs.isEmpty()) {
                arena.setGeneratorConfigs(genConfigs);
            }
        }

        if (config.contains("forge")) {
            arena.setForgeMaxLevel(config.getInt("forge.max-level", 10));
            List<ForgeLevel> levels = new java.util.ArrayList<>();
            ConfigurationSection forgeLevels = config.getConfigurationSection("forge.levels");
            if (forgeLevels != null) {
                for (String levelKey : forgeLevels.getKeys(false)) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        String levelPath = "forge.levels." + levelKey;
                        Map<Material, Long> intervals = new java.util.HashMap<>();
                        ConfigurationSection items = config.getConfigurationSection(levelPath);
                        if (items != null) {
                            for (String itemName : items.getKeys(false)) {
                                Material mat = Material.matchMaterial(itemName);
                                if (mat == null) {
                                    mat = Material.matchMaterial(itemName + "_INGOT");
                                }
                                long interval = config.getLong(levelPath + "." + itemName + ".interval", 0L);
                                if (mat != null && interval > 0L) {
                                    intervals.put(mat, interval);
                                }
                            }
                        }
                        if (!intervals.isEmpty()) {
                            levels.add(new ForgeLevel(level, intervals));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (!levels.isEmpty()) {
                arena.setForgeLevels(levels);
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
