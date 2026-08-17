package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.events.ArenaLoadEvent;
import dev.sebastianjnuwu.bedwars.api.events.ArenaSaveEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;

/**
 * Responsável pela persistência das arenas em disco: leitura do yml
 * ({@code arenas/<nome>.yml}), escrita, recarga e conversão de localizações.
 * Preserva seções do disco quando o mundo da arena ainda não está carregado.
 */
public final class ArenaPersistence {

    private final ArenaManager manager;

    /**
     * Cria o gerenciador de persistência de arenas.
     *
     * @param manager arena manager que será alcançado por este gerenciador (não nulo)
     */
    public ArenaPersistence(final ArenaManager manager) {
        this.manager = manager;
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
            arena.setLobby(this.parseLocationFor(config.getString("lobby"), targetWorld));
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
            arena.setArenaSpawn(this.parseLocationFor(config.getString("arena_spawn"), targetWorld));
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
        if (config.contains("spawn_item")) {
            final List<Material> spawnItems = new ArrayList<>();
            for (final String itemName : config.getStringList("spawn_item")) {
                final Material mat = Material.matchMaterial(itemName);
                if (mat != null) {
                    spawnItems.add(mat);
                }
            }
            arena.setSpawnItems(spawnItems);
        }
        if (config.contains("teams")) {
            for (final String key : config.getConfigurationSection("teams").getKeys(false)) {
                if (key.equalsIgnoreCase("min-players") || key.equalsIgnoreCase("max-players") || key.equalsIgnoreCase("min-teams")) {
                    continue;
                }
                final String path = "teams." + key;
                final var team = new dev.sebastianjnuwu.bedwars.model.ArenaTeam(key, config.getString(path + ".color"));
                if (config.contains(path + ".spawn")) {
                    team.setSpawn(this.parseLocationFor(config.getString(path + ".spawn"), targetWorld));
                }
                if (config.contains(path + ".spawn_block")) {
                    team.setSpawnBlockData(config.getString(path + ".spawn_block"));
                }
                if (config.contains(path + ".bed")) {
                    team.setBed(this.parseLocationFor(config.getString(path + ".bed"), targetWorld));
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
                final String type = config.getString(path + ".type");
                if (type == null) {
                    continue;
                }

                // Determine UUID: use the YAML key if valid UUID, otherwise generate new
                UUID genUuid;
                try {
                    genUuid = UUID.fromString(key);
                } catch (final IllegalArgumentException e) {
                    genUuid = UUID.randomUUID();
                }

                // A location fica null quando o mundo ainda não está carregado.
                // O gerador é mantido em memória; instâncias são lidas do disco.
                final Location loc = this.parseLocationFor(config.getString(path + ".location"), targetWorld);
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

        if (config.contains("enable-cmd")) {
            arena.setEnabledCommands(parseEnabledCommands(config.get("enable-cmd")));
        }

        if (config.contains("shop_npcs")) {
            List<ShopNpc> shopNpcs = new ArrayList<>();
            ConfigurationSection npcSection = config.getConfigurationSection("shop_npcs");
            if (npcSection != null) {
                String fallbackSkin = config.getString("shop_npcs.skin");
                String fallbackDisplayName = config.getString("shop_npcs.displayName");
                for (String key : npcSection.getKeys(false)) {
                    if (key.equals("skin") || key.equals("displayName")) {
                        continue;
                    }
                    Location loc = this.parseLocationFor(config.getString("shop_npcs." + key + ".location"), targetWorld);
                    if (loc == null) {
                        continue;
                    }
                    String skin = config.getString("shop_npcs." + key + ".skin", fallbackSkin);
                    String displayName = config.getString("shop_npcs." + key + ".displayName", fallbackDisplayName);
                    shopNpcs.add(new ShopNpc(loc, skin, displayName));
                }
            }
            if (!shopNpcs.isEmpty()) {
                arena.setShopNpcs(shopNpcs);
            }
        }

        if (config.contains("generator_config")) {
            Map<String, GeneratorConfig> genConfigs = new java.util.HashMap<>();
            for (String type : config.getConfigurationSection("generator_config").getKeys(false)) {
                String path = "generator_config." + type;
                String matName = config.getString(path + ".material");
                Material mat = matName != null ? Material.matchMaterial(matName) : null;
                Map<Integer, Long> levels = new java.util.HashMap<>();
                if (config.contains(path + ".levels")) {
                    for (String levelKey : config.getConfigurationSection(path + ".levels").getKeys(false)) {
                        levels.put(Integer.parseInt(levelKey), config.getLong(path + ".levels." + levelKey, 0L));
                    }
                }
                if (mat != null && !levels.isEmpty()) {
                    genConfigs.put(type, new GeneratorConfig(mat, levels));
                }
            }
            if (!genConfigs.isEmpty()) {
                arena.setGeneratorConfigs(genConfigs);
            }
        }

        if (config.contains("level-times")) {
            Map<Integer, Integer> levelTimes = new java.util.HashMap<>();
            for (String minuteKey : config.getConfigurationSection("level-times").getKeys(false)) {
                levelTimes.put(Integer.parseInt(minuteKey), config.getInt("level-times." + minuteKey, 1));
            }
            if (!levelTimes.isEmpty()) {
                arena.setLevelTimes(levelTimes);
            }
        }

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
        this.writeLocation(config, disk, "lobby", arena.getLobby());
        if (arena.getWorldName() != null) {
            config.set("world", arena.getWorldName());
        }
        if (arena.getMapName() != null) {
            config.set("map", arena.getMapName());
        }
        config.set("paste", arena.getPasteX() + "," + arena.getPasteY() + "," + arena.getPasteZ());
        config.set("schematic_size",
                arena.getSchematicWidth() + "," + arena.getSchematicHeight() + "," + arena.getSchematicLength());
        this.writeLocation(config, disk, "arena_spawn", arena.getArenaSpawn());
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

        if (arena.getSpawnItems() != null && !arena.getSpawnItems().isEmpty()) {
            final List<String> spawnItemNames = new ArrayList<>();
            for (final Material material : arena.getSpawnItems()) {
                spawnItemNames.add(material.name());
            }
            config.set("spawn_item", spawnItemNames);
        } else {
            config.set("spawn_item", null);
        }

        if (arena.getShop() != null) {
            config.set("shop", arena.getShop());
        }

        if (arena.getEnabledCommands() != null && !arena.getEnabledCommands().isEmpty()) {
            config.set("enable-cmd", arena.getEnabledCommands());
        } else {
            config.set("enable-cmd", null);
        }

        // Shop NPCs — se vazio porque o mundo não está carregado, preserva a seção do disco
        List<ShopNpc> shopNpcs = arena.getShopNpcs();
        if (shopNpcs != null && !shopNpcs.isEmpty()) {
            for (int i = 0; i < shopNpcs.size(); i++) {
                ShopNpc npc = shopNpcs.get(i);
                config.set("shop_npcs." + i + ".location", this.serializeLocation(npc.location()));
                if (npc.skin() != null) {
                    config.set("shop_npcs." + i + ".skin", npc.skin());
                }
                if (npc.displayName() != null) {
                    config.set("shop_npcs." + i + ".displayName", npc.displayName());
                }
            }
        } else if (disk != null && disk.contains("shop_npcs") && !this.sectionWorldLoaded(disk, "shop_npcs")) {
            this.copySection(disk, config, "shop_npcs");
        }

        // Generator configs
        for (var entry : arena.getGeneratorConfigs().entrySet()) {
            String type = entry.getKey();
            GeneratorConfig gc = entry.getValue();
            config.set("generator_config." + type + ".material", gc.material().name());
            config.set("generator_config." + type + ".levels", null);
            for (var levelEntry : gc.levels().entrySet()) {
                config.set("generator_config." + type + ".levels." + levelEntry.getKey(), levelEntry.getValue());
            }
        }

        // Level times
        config.set("level-times", null);
        if (arena.getLevelTimes() != null) {
            for (var entry : arena.getLevelTimes().entrySet()) {
                config.set("level-times." + entry.getKey(), entry.getValue());
            }
        }

        // Teams
        config.set("teams", null);
        for (final ArenaTeam team : arena.getTeams()) {
            final String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor());
            this.writeLocation(config, disk, path + ".spawn", team.getSpawn());
            if (team.getSpawnBlockData() != null) {
                config.set(path + ".spawn_block", team.getSpawnBlockData());
            }
            this.writeLocation(config, disk, path + ".bed", team.getBed());
            if (team.getBedFacing() != null) {
                config.set(path + ".bed_facing", team.getBedFacing());
            }
        }

        // Generators — usa UUID como chave, ignora location null.
        // Se nenhum gerador tem location resolvida (mundo não carregado), preserva a seção do disco.
        final boolean hasResolvedGenerator = arena.getGenerators().stream()
                .anyMatch(gen -> gen.getLocation() != null);
        if (hasResolvedGenerator) {
            config.set("generators", null);
            for (final ArenaGenerator gen : arena.getGenerators()) {
                if (gen.getLocation() == null) {
                    continue;
                }
                final String path = "generators." + gen.getUniqueId().toString();
                config.set(path + ".type", gen.getType());
                config.set(path + ".location", this.serializeLocation(gen.getLocation()));
                if (gen.getTeam() != null) {
                    config.set(path + ".team", gen.getTeam());
                }
                if (gen.getOriginBlockData() != null) {
                    config.set(path + ".origin_block", gen.getOriginBlockData());
                }
                if (gen.getOriginBlockDataAbove() != null) {
                    config.set(path + ".origin_block_above", gen.getOriginBlockDataAbove());
                }
            }
        } else if (disk != null && disk.contains("generators") && !this.sectionWorldLoaded(disk, "generators")) {
            this.copySection(disk, config, "generators");
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
            this.manager.diskConfigs.put(arenaName, config);
        } catch (final IOException e) {
            this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.save_arena_error", arenaName, e.getMessage()));
        }
    }

    /**
     * Grava uma localização, preservando o valor anterior quando ela está null
     * no cache (arena ainda sem mundo resolvido). Isso evita que um flush/save
     * com referências não resolvidas remova permanentemente as chaves do arquivo,
     * mesmo quando o mundo referenciado já está carregado.
     */
    private void writeLocation(final YamlConfiguration config, final YamlConfiguration disk,
                               final String path, final Location loc) {
        if (loc != null) {
            config.set(path, this.serializeLocation(loc));
            return;
        }
        if (disk != null && disk.contains(path)) {
            final String stored = disk.getString(path);
            if (stored != null && !stored.isBlank()) {
                config.set(path, stored);
                return;
            }
        }
        config.set(path, null);
    }

    /**
     * Verifica se alguma localização armazenada em uma seção do disco referencia
     * um mundo atualmente carregado.
     */
    private boolean sectionWorldLoaded(final YamlConfiguration disk, final String section) {
        final ConfigurationSection cs = disk != null ? disk.getConfigurationSection(section) : null;
        if (cs == null) {
            return false;
        }
        for (final String key : cs.getKeys(true)) {
            final Object value = cs.get(key);
            if (!(value instanceof final String str) || !str.contains(",")) {
                continue;
            }
            if (Bukkit.getWorld(str.split(",", 2)[0]) != null) {
                return true;
            }
        }
        return false;
    }

    private void copySection(final YamlConfiguration source, final YamlConfiguration target, final String section) {
        final ConfigurationSection cs = source.getConfigurationSection(section);
        if (cs == null) {
            return;
        }
        for (final String key : cs.getKeys(true)) {
            final Object value = source.get(section + "." + key);
            if (value instanceof ConfigurationSection) {
                continue;
            }
            target.set(section + "." + key, value);
        }
    }

    private String serializeLocation(final Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ()
                + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    private @Nullable Location parseLocationFor(final String str, final @Nullable World targetWorld) {
        if (targetWorld != null) {
            return this.rebaseLocation(str, targetWorld);
        }
        return this.parseLocation(str);
    }

    /**
     * Converte uma string de localização em um {@link Location} do mundo alvo,
     * mantendo as coordenadas originais (usado ao construir instâncias).
     *
     * @param str   string da localização (formato {@code mundo,x,y,z,yaw,pitch})
     * @param world mundo de partida alvo
     * @return localização rebaseada ou {@code null}
     */
    private @Nullable Location rebaseLocation(final String str, final World world) {
        if (str == null || str.isBlank()) {
            return null;
        }
        final String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }
        return new Location(world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                parts.length > 4 ? Float.parseFloat(parts[4]) : 0F,
                parts.length > 5 ? Float.parseFloat(parts[5]) : 0F);
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

    private static List<String> parseEnabledCommands(final Object raw) {
        List<String> commands = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (final Object item : list) {
                final String cmd = normalizeEnabledCommand(item);
                if (cmd != null) {
                    commands.add(cmd);
                }
            }
        } else {
            final String cmd = normalizeEnabledCommand(raw);
            if (cmd != null) {
                commands.add(cmd);
            }
        }
        return commands;
    }

    private static @Nullable String normalizeEnabledCommand(final Object raw) {
        if (raw == null) {
            return null;
        }
        String cmd = String.valueOf(raw).trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return null;
        }
        return cmd.toLowerCase();
    }
}