package dev.sebastianjnuwu.bedwars.manager;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * Cria o gerenciador de arenas.
     *
     * @param plugin     instância do plugin
     * @param mapsFolder pasta onde os schematics .bwmap são salvos
     */
    public ArenaManager(final JavaPlugin plugin, final File mapsFolder) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
    }

    /**
     * Carrega todas as arenas dos arquivos na pasta arenas/.
     */
    public void load() {
        this.arenas.clear();
        final File[] files = this.arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }
        for (final File file : files) {
            final String name = file.getName().replace(".yml", "");
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
            arena.setCountdown(config.getInt("countdown", 15));
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
                    final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(
                            config.getString(path + ".type"),
                            this.parseLocation(config.getString(path + ".location"))
                    );
                    if (config.contains(path + ".origin_block")) {
                        gen.setOriginBlockData(config.getString(path + ".origin_block"));
                    }
                    if (config.contains(path + ".origin_block_above")) {
                        gen.setOriginBlockDataAbove(config.getString(path + ".origin_block_above"));
                    }
                    arena.addGenerator(gen);
                }
            }

            this.arenas.put(name, arena);
        }
        this.plugin.getLogger().info("Arenas carregadas: " + this.arenas.size());
    }

    /**
     * Salva uma arena específica no arquivo arenas/&lt;nome&gt;.yml.
     *
     * @param arena arena a ser salva
     */
    public void save(final Arena arena) {
        final File file = new File(this.arenasFolder, arena.getName() + ".yml");
        final YamlConfiguration config = new YamlConfiguration();

        config.set("enabled", arena.isEnabled());

        if (arena.getLobby() != null) {
            config.set("lobby", this.serializeLocation(arena.getLobby()));
        }
        if (arena.getWorldName() != null) {
            config.set("world", arena.getWorldName());
        }
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        if (arena.getArenaSpawn() != null) {
            config.set("arena_spawn", this.serializeLocation(arena.getArenaSpawn()));
        }
        if (arena.getSpawnBlockData() != null) {
            config.set("spawn_block", arena.getSpawnBlockData());
        }
        config.set("min_players", arena.getMinPlayers());
        config.set("countdown", arena.getCountdown());

        final List<ArenaTeam> teams = arena.getTeams();
        for (int i = 0; i < teams.size(); i++) {
            final ArenaTeam team = teams.get(i);
            final String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor());
            if (team.getSpawn() != null) {
                config.set(path + ".spawn", this.serializeLocation(team.getSpawn()));
            }
            if (team.getSpawnBlockData() != null) {
                config.set(path + ".spawn_block", team.getSpawnBlockData());
            }
            if (team.getBed() != null) {
                config.set(path + ".bed", this.serializeLocation(team.getBed()));
            }
            if (team.getBedFacing() != null) {
                config.set(path + ".bed_facing", team.getBedFacing());
            }
        }

        final List<ArenaGenerator> generators = arena.getGenerators();
        for (int i = 0; i < generators.size(); i++) {
            final ArenaGenerator gen = generators.get(i);
            final String path = "generators." + i;
            config.set(path + ".type", gen.getType());
            config.set(path + ".location", this.serializeLocation(gen.getLocation()));
            if (gen.getOriginBlockData() != null) {
                config.set(path + ".origin_block", gen.getOriginBlockData());
            }
            if (gen.getOriginBlockDataAbove() != null) {
                config.set(path + ".origin_block_above", gen.getOriginBlockDataAbove());
            }
        }

        try {
            config.save(file);
        } catch (final IOException e) {
            this.plugin.getLogger().severe("Erro ao salvar arena " + arena.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Cria uma nova arena com o nome especificado.
     *
     * @param name nome da arena
     * @return a arena criada, ou null se já existir
     */
    public Arena create(final String name) {
        if (this.arenas.containsKey(name)) {
            return null;
        }
        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        this.arenas.put(name, arena);
        this.save(arena);
        return arena;
    }

    /**
     * Deleta uma arena e seus arquivos.
     *
     * @param name nome da arena
     * @return true se foi deletada
     */
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

    /**
     * Retorna uma arena pelo nome.
     *
     * @param name nome da arena
     * @return a arena ou null
     */
    public Arena get(final String name) {
        return this.arenas.get(name);
    }

    /**
     * Retorna todas as arenas registradas.
     *
     * @return conjunto de nomes
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(this.arenas.keySet());
    }

    public Collection<Arena> getAll() {
        return Collections.unmodifiableCollection(this.arenas.values());
    }

    private String serializeLocation(final Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ()
                + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private Location parseLocation(final String str) {
        final String[] parts = str.split(",");
        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
        );
    }
}
