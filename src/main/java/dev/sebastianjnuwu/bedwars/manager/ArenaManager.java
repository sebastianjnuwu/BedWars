package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.Saveable;
import dev.sebastianjnuwu.bedwars.api.events.ArenaLoadEvent;
import dev.sebastianjnuwu.bedwars.api.events.ArenaSaveEvent;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import dev.sebastianjnuwu.bedwars.world.VoidGenerator;
import dev.sebastianjnuwu.bedwars.world.WorldManager;

/**
 * Gerencia todas as arenas do servidor.
 * Cada arena é salva em um arquivo separado: arenas/<nome>.yml.
 */
public class ArenaManager implements dev.sebastianjnuwu.bedwars.api.ArenaManager, Saveable {

    private final JavaPlugin plugin;
    private final Map<String, Arena> arenas;
    private final File arenasFolder;
    private final File mapsFolder;
    private final WorldManager worldManager;
    private final LangManager lang;
    private final Map<String, YamlConfiguration> diskConfigs;
    private final Set<String> cleanWorlds;
    private final Map<String, Integer> instanceCounters;

    public ArenaManager(final JavaPlugin plugin, final WorldManager worldManager, final File mapsFolder) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.arenas = new HashMap<>();
        this.diskConfigs = new HashMap<>();
        this.worldManager = worldManager;
        this.mapsFolder = mapsFolder;
        this.arenasFolder = new File(plugin.getDataFolder(), "arenas");
        this.arenasFolder.mkdirs();
        this.cleanWorlds = new HashSet<>();
        this.instanceCounters = new HashMap<>();
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
        if (arena.getShopNpcs() != null) {
            final List<ShopNpc> updated = new ArrayList<>();
            for (final ShopNpc npc : arena.getShopNpcs()) {
                final Location old = npc.location();
                final Location newLoc = new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch());
                updated.add(new ShopNpc(newLoc, npc.skin(), npc.displayName()));
            }
            arena.setShopNpcs(updated);
        }
    }

    public boolean resetArenaMap(final @org.jetbrains.annotations.NotNull String name) {
        final Arena arena = this.get(name);
        if (arena == null) {
            return false;
        }
        final String worldName = "bw_" + name;
        final World world = this.buildWorld(name, worldName, arena, "log.arena_manager.reset_error");
        if (world == null) {
            return false;
        }
        arena.setWorldName(worldName);
        this.updateWorldReferences(arena, world);
        this.restoreBeds(world, arena);
        this.flush(arena.getName());
        this.showMarkerBlocks(this.get(name));
        return true;
    }

    /**
     * Cria uma nova instância de partida para uma arena, clonando a configuração
     * e construindo um mundo de partida dedicado ({@code bw_<arena>_<id>}).
     * <p>
     * Isso permite que uma única arena hospede várias partidas simultâneas do
     * mesmo mapa, cada uma isolada em seu próprio mundo, recriado a partir do
     * schematic a cada instância.
     * </p>
     *
     * @param arenaName nome da arena (não nulo)
     * @return a arena clonada com o mundo de partida pronto, ou {@code null} se falhar
     */
    public @Nullable Arena createInstance(final String arenaName) {
        final Arena arena = this.get(arenaName);
        if (arena == null) {
            return null;
        }
        final File file = new File(this.arenasFolder, arenaName + ".yml");
        if (!file.exists()) {
            return null;
        }
        final String worldName = this.nextInstanceWorldName(arenaName);

        // Mundo construído com as configurações não-posicionais da arena em memória
        // (paste, mapa, difficulty... — independem de locations resolvidas).
        final World world = this.buildWorld(arenaName, worldName, arena, "log.arena_manager.load_error");
        if (world == null) {
            return null;
        }
        // Instância reconstruída 100% do disco, com todas as locations rebasadas
        // para o mundo de partida recém-criado.
        final Arena instance = this.loadArenaFromFile(arenaName, file, world);
        instance.setWorldName(worldName);
        this.restoreBeds(world, instance);
        return instance;
    }

    /**
     * Cria uma instância de partida de forma assíncrona, colando o schematic
     * fora da main thread (FAWE enfileira as mudanças de blocos) e invocando o
     * callback na main thread quando o mundo estiver pronto.
     * <p>
     * O mundo é criado (ou reutilizado) na main thread e o paste acontece em
     * segundo plano. Quando finalizado, o callback recebe a arena clonada com
     * todas as locations rebasadas, ou {@code null} se a construção falhar.
     * </p>
     *
     * @param arenaName nome da arena (não nulo)
     * @param callback  consumidor chamado na main thread com a instância pronta (não nulo)
     */
    public void createInstanceAsync(final String arenaName, final @org.jetbrains.annotations.NotNull java.util.function.Consumer<Arena> callback) {
        final Arena arena = this.get(arenaName);
        if (arena == null) {
            callback.accept(null);
            return;
        }
        final File file = new File(this.arenasFolder, arenaName + ".yml");
        if (!file.exists()) {
            callback.accept(null);
            return;
        }
        final File mapFile = this.getMapFile(arena);
        if (mapFile == null) {
            callback.accept(null);
            return;
        }
        final String worldName = this.nextInstanceWorldName(arenaName);
        final World world = this.createOrLoadWorld(worldName);
        if (world == null) {
            callback.accept(null);
            return;
        }

        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Schematic schematic;
            final Location pasteLocation;
            try {
                schematic = Schematic.load(arenaName, mapFile);
                pasteLocation = this.pasteSchematic(world, schematic, mapFile, arena);
            } catch (final Exception e) {
                this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.load_error", arenaName, e.getMessage()));
                this.markWorldDirty(worldName);
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> callback.accept(null));
                return;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                try {
                    this.finalizeWorld(world, arena, schematic, pasteLocation);
                    final Arena instance = this.loadArenaFromFile(arenaName, file, world);
                    instance.setWorldName(worldName);
                    this.restoreBeds(world, instance);
                    this.markWorldClean(worldName);
                    callback.accept(instance);
                } catch (final Exception e) {
                    this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.load_error", arenaName, e.getMessage()));
                    this.markWorldDirty(worldName);
                    callback.accept(null);
                }
            });
        });
    }

    /**
     * Remove o mundo de uma instância de partida (unload + exclusão do disco).
     *
     * @param worldName nome do mundo de partida
     */
    public void deleteInstanceWorld(final String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        this.worldManager.deleteWorld(worldName);
        this.markWorldDirty(worldName);
    }

    private String nextInstanceWorldName(final String arenaName) {
        int id = this.instanceCounters.getOrDefault(arenaName, 0);
        String worldName;
        do {
            worldName = "bw_" + arenaName + "_" + id;
            id++;
        } while (new File(Bukkit.getWorldContainer(), worldName).exists());
        this.instanceCounters.put(arenaName, id);
        return worldName;
    }

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
                    try {
                        world.setTime(Long.parseLong(arena.getTime()));
                    } catch (final NumberFormatException ignored) {
                    }
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
                default -> {
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

    /**
     * Resolve o arquivo de mapa usado por uma arena, considerando o mapa
     * compartilhado ({@link Arena#getMapName()}) quando configurado. Isso
     * permite que várias arenas rodem partidas simultâneas do mesmo mapa.
     *
     * @param arena arena cujo mapa deve ser resolvido (não nulo)
     * @return arquivo do schematic, ou {@code null} se não encontrado
     */
    public @Nullable File getMapFile(final Arena arena) {
        if (arena == null) {
            return null;
        }
        final String mapName = arena.getMapName();
        final String resolved = mapName == null || mapName.isBlank() ? arena.getName() : mapName;
        return this.getMapFile(resolved);
    }

    /**
     * Garante que o mundo da arena está carregado e com as referências atualizadas.
     * <p>
     * Quando o mundo já está carregado e sem mudanças pendentes (ex.: após um
     * unload/load do plugin), a arena é recarregada do yml e suas localizações
     * (spawns, camas, geradores e NPCs) são re-sincronizadas em memória. Isso evita
     * que o {@code validateArena} acuse configurações faltando após o reload.
     * Caso contrário, o mundo é reconstruído a partir do schematic.
     * </p>
     *
     * @param arena a arena cujo mundo deve estar pronto (não nulo)
     * @return o mundo carregado/pronto, ou {@code null} se não foi possível
     */
    public @Nullable World ensureWorldLoaded(final Arena arena) {
        String worldName = arena.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            worldName = "bw_" + arena.getName();
        }
        World world = Bukkit.getWorld(worldName);
        // Mundo já carregado e sem mudanças pendentes (ex.: após unload/load do plugin).
        // Recarrega o yml e atualiza as referências em memória para que spawns, camas,
        // geradores e NPCs não fiquem nulos e o validateArena não acuse configuração faltando.
        if (world != null && this.cleanWorlds.contains(worldName)) {
            this.applyWorldSettings(world, arena);
            this.reload(arena.getName());
            final Arena refreshed = this.get(arena.getName());
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
                this.updateWorldReferences(refreshed, world);
                this.restoreBeds(world, refreshed);
            }
            this.flush(arena.getName());
            return world;
        }
        world = this.buildWorld(arena.getName(), worldName, arena, "log.arena_manager.load_error");
        if (world == null) {
            return null;
        }
        this.reload(arena.getName());
        final Arena refreshed = this.get(arena.getName());
        if (refreshed != null) {
            refreshed.setWorldName(worldName);
            this.updateWorldReferences(refreshed, world);
            this.restoreBeds(world, refreshed);
        }
        this.flush(arena.getName());
        return world;
    }

    /**
     * Reconstrói o mundo da arena a partir do schematic do mapa.
     * <p>
     * Quando a remoção completa do mundo falha (arquivos travados, ex.: Windows),
     * o schematic é colado por cima do mundo existente para restaurar camas,
     * minérios e demais blocos, sem depender da exclusão do diretório.
     * </p>
     *
     * @param name      nome da arena
     * @param worldName nome do mundo de partida
     * @param arena     arena cujas posições de paste são usadas
     * @param errorKey  chave de log usada em caso de falha
     * @return o mundo reconstruído, ou {@code null} se não foi possível
     */
    private @Nullable World buildWorld(final String name, final String worldName, final Arena arena, final String errorKey) {
        final File mapFile = this.getMapFile(arena);
        if (mapFile == null) {
            return null;
        }
        try {
            final World world = this.createOrLoadWorld(worldName);
            if (world == null) {
                return null;
            }
            final Schematic schematic = Schematic.load(name, mapFile);
            final Location pasteLocation = this.pasteSchematic(world, schematic, mapFile, arena);
            this.finalizeWorld(world, arena, schematic, pasteLocation);
            this.markWorldClean(worldName);
            return world;
        } catch (final Exception e) {
            this.plugin.getLogger().severe(this.lang.raw(errorKey, name, e.getMessage()));
            this.markWorldDirty(worldName);
            return null;
        }
    }

    /**
     * Obtém o mundo de partida já carregado ou cria um novo mundo vazio (void).
     * <p>
     * Reutiliza o mundo já carregado em vez de apagar e recriar do zero. Apagar
     * e recriar exige que o Paper gere um mundo novo (depende do
     * config/paper-world-defaults.yml), o que falha em servidores sem esse
     * arquivo. Colar o schematic por cima do mundo existente restaura camas,
     * minérios e blocos sem precisar de um novo mundo.
     * </p>
     *
     * @param worldName nome do mundo de partida
     * @return o mundo carregado/criado, ou {@code null} se não foi possível
     */
    private @Nullable World createOrLoadWorld(final String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            final WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidGenerator());
            world = wc.createWorld();
        }
        if (world == null) {
            this.markWorldDirty(worldName);
            return null;
        }
        return world;
    }

    /**
     * Cola o schematic do mapa no mundo, retornando a localização base do paste.
     *
     * @param world          mundo de destino
     * @param schematic      schematic carregado
     * @param mapFile        arquivo do mapa
     * @param arena          arena cujas posições de paste são usadas
     * @return localização base do paste
     * @throws Exception se o paste falhar
     */
    private Location pasteSchematic(final World world, final Schematic schematic, final File mapFile, final Arena arena) throws Exception {
        final Location pasteLocation = new Location(
                world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
        schematic.paste(world, pasteLocation, mapFile);
        return pasteLocation;
    }

    /**
     * Finaliza o mundo de partida após o paste: limpa entidades, ajusta o spawn
     * e aplica as configurações de mundo da arena.
     *
     * @param world         mundo de partida
     * @param arena         arena cujas configurações são aplicadas
     * @param schematic     schematic usado no paste
     * @param pasteLocation localização base do paste
     */
    private void finalizeWorld(final World world, final Arena arena, final Schematic schematic, final Location pasteLocation) {
        this.clearWorldEntities(world, pasteLocation, schematic);
        world.setSpawnLocation(pasteLocation.getBlockX(), pasteLocation.getBlockY(), pasteLocation.getBlockZ());
        this.applyWorldSettings(world, arena);
    }

    private void clearWorldEntities(final World world, final Location min, final Schematic schematic) {
        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = minX + schematic.getWidth() - 1;
        final int maxY = minY + schematic.getHeight() - 1;
        final int maxZ = minZ + schematic.getLength() - 1;
        for (final Entity entity : world.getEntities()) {
            if (entity instanceof Player) {
                continue;
            }
            final Location loc = entity.getLocation();
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX
                    && loc.getBlockY() >= minY && loc.getBlockY() <= maxY
                    && loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                entity.remove();
            }
        }
    }

    /**
     * Restaura as camas das equipes após a reconstrução do mundo.
     * <p>
     * Como a cama pode ficar fora dos limites do schematic, ela é recolocada
     * programaticamente a partir da configuração da arena (local + direção).
     * </p>
     */
    private void restoreBeds(final World world, final Arena arena) {
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getBed() == null || team.getBedFacing() == null) {
                continue;
            }
            final BlockFace face;
            try {
                face = BlockFace.valueOf(team.getBedFacing().toUpperCase());
            } catch (final IllegalArgumentException ignored) {
                continue;
            }
            final Material material = this.getBedMaterial(team.getColor());
            final Location foot = new Location(world, team.getBed().getBlockX(), team.getBed().getBlockY(), team.getBed().getBlockZ());
            final Bed footData = (Bed) Bukkit.createBlockData(material);
            footData.setFacing(face);
            footData.setPart(Bed.Part.FOOT);
            foot.getBlock().setBlockData(footData, false);
            final Bed headData = (Bed) Bukkit.createBlockData(material);
            headData.setFacing(face);
            headData.setPart(Bed.Part.HEAD);
            final Location head = foot.clone().add(face.getModX(), face.getModY(), face.getModZ());
            head.getBlock().setBlockData(headData, false);
        }
    }

    private Material getBedMaterial(final String dyeColor) {
        if (dyeColor == null) {
            return Material.RED_BED;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_BED;
            case "BLUE", "AZUL" -> Material.BLUE_BED;
            case "GREEN", "VERDE" -> Material.GREEN_BED;
            case "YELLOW", "AMARELO" -> Material.YELLOW_BED;
            case "PURPLE", "ROXO" -> Material.PURPLE_BED;
            case "PINK", "ROSA" -> Material.PINK_BED;
            case "ORANGE", "LARANJA" -> Material.ORANGE_BED;
            case "CYAN", "CIANO" -> Material.CYAN_BED;
            case "LIME", "VERDE_LIMA" -> Material.LIME_BED;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_BED;
            case "GRAY", "CINZA" -> Material.GRAY_BED;
            case "BLACK", "PRETO" -> Material.BLACK_BED;
            default -> Material.RED_BED;
        };
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
        if (dyeColor == null) {
            return Material.WHITE_CONCRETE;
        }
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
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_CONCRETE;
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

    @Override
    public void save() {
        this.flush();
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
        final YamlConfiguration disk = this.diskConfigs.get(arenaName);

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
        config.set("min_players", arena.getMinPlayers());
        config.set("countdown", arena.getCountdown());
        config.set("respawn-delay", arena.getRespawnDelay());

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

        if (arena.getShop() != null) {
            config.set("shop", arena.getShop());
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
            config.set("generator_config." + type + ".interval", gc.interval());
        }

        // Forge levels
        config.set("forge.max-level", arena.getForgeMaxLevel());
        config.set("forge.level-default", arena.getForgeDefaultLevel());
        for (ForgeLevel fl : arena.getForgeLevels()) {
            String levelPath = "forge.levels." + fl.level();
            if (fl.upgradeMaterial() != null && fl.upgradePrice() > 0) {
                config.set(levelPath + ".upgrade.price", fl.upgradePrice());
                config.set(levelPath + ".upgrade.material", fl.upgradeMaterial().name().toLowerCase().replace("_ingot", ""));
            }
            for (var entry : fl.intervals().entrySet()) {
                config.set(levelPath + "." + entry.getKey().name().toLowerCase() + ".interval", entry.getValue());
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
            this.diskConfigs.put(arenaName, config);
        } catch (final IOException e) {
            this.plugin.getLogger().severe(this.lang.raw("log.arena_manager.save_arena_error", arenaName, e.getMessage()));
        }
    }

    /**
     * Grava uma localização, preservando o valor anterior quando ela está null
     * apenas porque o mundo referenciado ainda não foi carregado. Isso evita que
     * um flush/save com o mundo descarregado remova permanentemente as chaves
     * do arquivo. Remoções intencionais (feitas com o mundo carregado) continuam
     * sendo persistidas como null.
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
                final String storedWorld = stored.split(",", 2)[0];
                if (Bukkit.getWorld(storedWorld) == null) {
                    config.set(path, stored);
                    return;
                }
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

    public Arena create(final String name) {
        if (this.arenas.containsKey(name)) {
            return null;
        }
        final var arena = new dev.sebastianjnuwu.bedwars.model.Arena(name);
        arena.setMinPlayers(2);
        arena.setCountdown(3);
        arena.setRespawnDelay(3);
        // Default generator configs
        Map<String, GeneratorConfig> genConfigs = new java.util.HashMap<>();
        genConfigs.put("iron", new GeneratorConfig(Material.IRON_INGOT, 40));
        genConfigs.put("gold", new GeneratorConfig(Material.GOLD_INGOT, 120));
        genConfigs.put("diamond", new GeneratorConfig(Material.DIAMOND, 600));
        genConfigs.put("emerald", new GeneratorConfig(Material.EMERALD, 1200));
        arena.setGeneratorConfigs(genConfigs);
        // Default forge levels
        arena.setForgeMaxLevel(10);
        arena.setForgeDefaultLevel(1);
        List<ForgeLevel> forgeLevels = new java.util.ArrayList<>();
        forgeLevels.add(new ForgeLevel(1, Map.of(Material.IRON_INGOT, 20L), 3, Material.IRON_INGOT));
        forgeLevels.add(new ForgeLevel(2, Map.of(Material.IRON_INGOT, 18L, Material.GOLD_INGOT, 100L), 5, Material.IRON_INGOT));
        forgeLevels.add(new ForgeLevel(3, Map.of(Material.IRON_INGOT, 16L, Material.GOLD_INGOT, 90L), 8, Material.IRON_INGOT));
        forgeLevels.add(new ForgeLevel(4, Map.of(Material.IRON_INGOT, 14L, Material.GOLD_INGOT, 80L, Material.DIAMOND, 1200L), 10, Material.IRON_INGOT));
        forgeLevels.add(new ForgeLevel(5, Map.of(Material.IRON_INGOT, 12L, Material.GOLD_INGOT, 70L, Material.DIAMOND, 1000L), 15, Material.IRON_INGOT));
        forgeLevels.add(new ForgeLevel(6, Map.of(Material.IRON_INGOT, 10L, Material.GOLD_INGOT, 60L, Material.DIAMOND, 800L, Material.EMERALD, 2400L), 20, Material.GOLD_INGOT));
        forgeLevels.add(new ForgeLevel(7, Map.of(Material.IRON_INGOT, 8L, Material.GOLD_INGOT, 50L, Material.DIAMOND, 700L, Material.EMERALD, 2000L), 25, Material.GOLD_INGOT));
        forgeLevels.add(new ForgeLevel(8, Map.of(Material.IRON_INGOT, 6L, Material.GOLD_INGOT, 40L, Material.DIAMOND, 600L, Material.EMERALD, 1600L), 30, Material.GOLD_INGOT));
        forgeLevels.add(new ForgeLevel(9, Map.of(Material.IRON_INGOT, 5L, Material.GOLD_INGOT, 30L, Material.DIAMOND, 500L, Material.EMERALD, 1400L), 40, Material.GOLD_INGOT));
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
        this.diskConfigs.remove(name);

        final File configFile = new File(this.arenasFolder, name + ".yml");
        if (configFile.exists()) {
            configFile.delete();
        }

        final String mapName = arena.getMapName();
        if (mapName == null || mapName.isBlank() || mapName.equals(name)) {
            final File mapFile = this.getMapFile(name);
            if (mapFile != null) {
                mapFile.delete();
            }
        }

        this.worldManager.deleteWorld("bw_" + name);
        this.worldManager.deleteWorld("bw_" + name + "_edit");
        this.markWorldDirty("bw_" + name);
        this.markWorldDirty("bw_" + name + "_edit");

        final File template = this.worldManager.getTemplateFolder(name);
        if (template.exists()) {
            deleteDirectory(template);
        }

        return true;
    }

    private void deleteDirectory(final File path) {
        if (!path.exists()) {
            return;
        }
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
        return this.loadArenaFromFile(name, file, null);
    }

    private Arena loadArenaFromFile(final String name, final File file, final @Nullable World targetWorld) {
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
        arena.setMinPlayers(config.getInt("min_players", 2));
        arena.setCountdown(config.getInt("countdown", 3));
        arena.setRespawnDelay(config.getInt("respawn-delay", 3));
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

        if (config.contains("shop_npcs")) {
            List<ShopNpc> shopNpcs = new java.util.ArrayList<>();
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
            arena.setForgeDefaultLevel(config.getInt("forge.level-default", 1));
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
                            int upgradePrice = config.getInt(levelPath + ".upgrade.price", 0);
                            Material upgradeMaterial = forgeCurrencyMaterial(config.getString(levelPath + ".upgrade.material"));
                            levels.add(new ForgeLevel(level, intervals, upgradePrice, upgradeMaterial));
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (!levels.isEmpty()) {
                arena.setForgeLevels(levels);
            }
        }

        if (targetWorld == null) {
            final World loadWorld = arena.getWorldName() != null ? Bukkit.getWorld(arena.getWorldName()) : null;
            Bukkit.getPluginManager().callEvent(new ArenaLoadEvent(arena, loadWorld));
            this.diskConfigs.put(name, config);
        }
        return arena;
    }

    private static @Nullable Material forgeCurrencyMaterial(final @Nullable String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim().toLowerCase()) {
            case "iron", "iron_ingot" -> Material.IRON_INGOT;
            case "gold", "gold_ingot" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "emerald" -> Material.EMERALD;
            default -> null;
        };
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
