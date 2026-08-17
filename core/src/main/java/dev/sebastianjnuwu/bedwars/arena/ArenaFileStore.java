package dev.sebastianjnuwu.bedwars.arena;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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

/**
 * Persistência das arenas em arquivos YAML dentro da pasta {@code arenas}.
 * <p>
 * Responsável por salvar e carregar o estado completo de uma {@link Arena}
 * (spawns, times, camas, geradores e configurações de mundo).
 * </p>
 */
class ArenaFileStore {

    private final LangManager lang;
    private final File arenasFolder;

    ArenaFileStore(@NotNull LangManager lang, @NotNull File arenasFolder) {
        this.lang = lang;
        this.arenasFolder = arenasFolder;
    }

    /**
     * Carrega uma arena a partir de um arquivo YAML.
     *
     * @param name nome da arena
     * @param file arquivo de origem
     * @return arena carregada ou {@code null} em caso de erro
     */
    @Nullable Arena load(@NotNull String name, @NotNull File file) {
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
        arena.setMinPlayersPerTeam(config.getInt("teams.min-players", 1));
        arena.setMaxPlayersPerTeam(config.getInt("teams.max-players", 0));
        arena.setMinTeamsToStart(config.getInt("teams.min-teams", 2));
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
                if (key.equalsIgnoreCase("min-players") || key.equalsIgnoreCase("max-players") || key.equalsIgnoreCase("min-teams")) {
                    continue;
                }
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

    /**
     * Salva uma arena no arquivo YAML.
     *
     * @param arena arena a salvar
     */
    void save(@NotNull Arena arena) {
        final File file = new File(arenasFolder, arena.getName() + ".yml");
        final YamlConfiguration config = new YamlConfiguration();

        config.set("enabled", arena.isEnabled());
        setIfNotNull(config, "lobby", serializeLocation(arena.getLobby()));
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        setIfNotNull(config, "arena_spawn", serializeLocation(arena.getArenaSpawn()));
        setIfNotNull(config, "spawn_block", arena.getSpawnBlockData());
        config.set("teams.min-players", arena.getMinPlayersPerTeam());
        config.set("teams.max-players", arena.getMaxPlayersPerTeam());
        config.set("teams.min-teams", arena.getMinTeamsToStart());
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