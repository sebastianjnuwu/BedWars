package dev.sebastianjnuwu.bedwars.manager;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.admin.team.SetSpawnCommand;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import dev.sebastianjnuwu.bedwars.world.VoidGenerator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gerencia todas as arenas do servidor.
 * Cada arena é salva em um arquivo separado: arenas/&lt;nome&gt;.yml.
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFolder;
    private final File mapsFolder;

    public ArenaManager(final JavaPlugin plugin, final File mapsFolder) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
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

    /**
     * Repasta o schematic e recarrega a configuração da arena após uma partida.
     */
    public boolean resetArenaMap(final String name) {
        final Arena arena = this.get(name);
        if (arena == null) {
            return false;
        }
        final File mapFile = this.getMapFile(name);
        if (mapFile == null) {
            return false;
        }
        final World world = this.ensureWorldLoaded(arena);
        if (world == null) {
            return false;
        }
        try {
            this.removeAllGeneratorHolograms(arena);
            final Schematic schematic = Schematic.load(mapFile);
            final Location pasteLocation = LocationUtil.getPasteLocation(arena, world);
            schematic.paste(pasteLocation);
            // Reload first so showMarkerBlocks uses fresh data from disk
            this.reload(name);
            this.showMarkerBlocks(this.get(name));
            // Do NOT save here — the file already has correct generator data;
            // saving would risk overwriting it with a partially-populated in-memory object.
            return true;
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao resetar arena " + name + ": " + e.getMessage());
            return false;
        }
    }

    public void removeAllGeneratorHolograms(final Arena arena) {
        final String worldName = arena.getWorldName();
        if (worldName == null) return;
        final World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        world.getEntitiesByClass(ArmorStand.class).stream()
                .filter(stand -> stand.getScoreboardTags().contains("bedwars_generator_hologram"))
                .forEach(ArmorStand::remove);
    }

    public @Nullable File getMapFile(final String name) {
        File file = new File(this.mapsFolder, name + ".schem");
        if (!file.exists()) {
            file = new File(this.mapsFolder, name + ".schematic");
        }
        if (!file.exists()) {
            file = new File(this.mapsFolder, name + ".bwmap");
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
            final Schematic schematic = Schematic.load(mapFile);
            final Location pasteLocation = LocationUtil.getPasteLocation(arena, world);
            schematic.paste(pasteLocation);
            world.setSpawnLocation(pasteLocation.getBlockX(), pasteLocation.getBlockY(), pasteLocation.getBlockZ());
            arena.setWorldName(worldName);
            this.save(arena);
            this.reload(arena.getName());
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
            
            // Criar holograma do gerador
            this.createGeneratorHologram(generator);
        }
    }

    private void createGeneratorHologram(final ArenaGenerator generator) {
        if (generator.getLocation() == null) return;
        
        final Location location = generator.getLocation().clone().add(0.5, 2.0, 0.5);
        final org.bukkit.entity.ArmorStand hologram = (org.bukkit.entity.ArmorStand) location.getWorld().spawnEntity(location, org.bukkit.entity.EntityType.ARMOR_STAND);
        
        hologram.setInvisible(true);
        hologram.setMarker(true);
        hologram.setGravity(false);
        hologram.setCustomNameVisible(false);
        hologram.addScoreboardTag("bedwars_generator_hologram");
        hologram.getEquipment().setHelmet(new ItemStack(this.getHologramItemMaterial(generator.getType())));
    }

    private Material getHologramItemMaterial(final String type) {
        return switch (type.toLowerCase()) {
            case "iron" -> Material.IRON_INGOT;
            case "gold" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            case "forge" -> Material.FURNACE;
            default -> Material.STONE;
        };
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
        final File file = new File(this.arenasFolder, arena.getName() + ".yml");
        
        // Load existing file to preserve data not in memory (e.g. locations when world is unloaded)
        final YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();

        config.set("enabled", arena.isEnabled());

        setIfNotNull(config, "lobby", arena.getLobby() != null ? this.serializeLocation(arena.getLobby()) : null);
        setIfNotNull(config, "world", arena.getWorldName());
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        setIfNotNull(config, "arena_spawn", arena.getArenaSpawn() != null ? this.serializeLocation(arena.getArenaSpawn()) : null);
        setIfNotNull(config, "spawn_block", arena.getSpawnBlockData());
        config.set("min_players", arena.getMinPlayers());
        config.set("countdown", arena.getCountdown());

        // Replace entire teams section
        config.set("teams", null);
        for (final ArenaTeam team : arena.getTeams()) {
            final String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor());
            setIfNotNull(config, path + ".spawn", team.getSpawn() != null ? this.serializeLocation(team.getSpawn()) : null);
            setIfNotNull(config, path + ".spawn_block", team.getSpawnBlockData());
            setIfNotNull(config, path + ".bed", team.getBed() != null ? this.serializeLocation(team.getBed()) : null);
            setIfNotNull(config, path + ".bed_facing", team.getBedFacing());
        }

        // Replace entire generators section
        config.set("generators", null);
        final List<ArenaGenerator> generators = arena.getGenerators();
        for (int i = 0; i < generators.size(); i++) {
            final ArenaGenerator gen = generators.get(i);
            final String path = "generators." + i;
            config.set(path + ".type", gen.getType());
            setIfNotNull(config, path + ".location", gen.getLocation() != null ? this.serializeLocation(gen.getLocation()) : null);
            setIfNotNull(config, path + ".team", gen.getTeam());
            setIfNotNull(config, path + ".origin_block", gen.getOriginBlockData());
            setIfNotNull(config, path + ".origin_block_above", gen.getOriginBlockDataAbove());
        }

        try {
            config.save(file);
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao salvar arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    private void setIfNotNull(final YamlConfiguration config, final String path, final Object value) {
        if (value != null) {
            config.set(path, value);
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

        return true;
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
                if (gen.getLocation() == null) {
                    continue;
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
