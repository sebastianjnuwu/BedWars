package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Backend de mundos baseado em Schematic/FAWE (sistema ativo).
 * <p>
 * Cria mundos de partida vazios (void) via {@link WorldCreator} +
 * {@link VoidGenerator}, cola o schematic do mapa e finaliza o mundo
 * (limpeza de entidades, spawn e configurações da arena). É a implementação
 * usada em produção pelo plugin; a colagem assíncrona enfileira as mudanças
 * de blocos no FAWE e conclui na main thread.
 * </p>
 */
public class SchematicWorldProvider implements WorldProvider {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final WorldManager worldManager;

    public SchematicWorldProvider(final JavaPlugin plugin, final LangManager lang, final WorldManager worldManager) {
        this.plugin = plugin;
        this.lang = lang;
        this.worldManager = worldManager;
    }

    @Override
    public String id() {
        return "schematic";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public @Nullable World buildWorld(final String name, final String worldName, final @Nullable File mapFile,
                                      final Arena arena, final String errorKey) {
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
            return world;
        } catch (final Exception e) {
            this.plugin.getLogger().severe(this.lang.raw(errorKey, name, e.getMessage()));
            return null;
        }
    }

    @Override
    public void buildWorldAsync(final String name, final String worldName, final @Nullable File mapFile,
                                final Arena arena, final String errorKey, final Consumer<@Nullable World> callback) {
        if (mapFile == null) {
            callback.accept(null);
            return;
        }
        final World world = this.createOrLoadWorld(worldName);
        if (world == null) {
            callback.accept(null);
            return;
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final Schematic schematic;
            final Location pasteLocation;
            try {
                schematic = Schematic.load(name, mapFile);
                pasteLocation = this.pasteSchematic(world, schematic, mapFile, arena);
            } catch (final Exception e) {
                this.plugin.getLogger().severe(this.lang.raw(errorKey, name, e.getMessage()));
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> callback.accept(null));
                return;
            }
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                try {
                    this.finalizeWorld(world, arena, schematic, pasteLocation);
                    callback.accept(world);
                } catch (final Exception e) {
                    this.plugin.getLogger().severe(this.lang.raw(errorKey, name, e.getMessage()));
                    callback.accept(null);
                }
            });
        });
    }

    @Override
    public boolean deleteWorld(final String worldName) {
        return this.worldManager.deleteWorld(worldName);
    }

    @SuppressWarnings("deprecation")
    @Override
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

    @Override
    public boolean templateExists(final String name) {
        return this.worldManager.templateExists(name);
    }

    @Override
    public void saveTemplate(final String name, final World world) throws IOException {
        this.worldManager.saveTemplate(name, world);
    }

    @Override
    public @Nullable File getTemplateFolder(final String name) {
        return this.worldManager.getTemplateFolder(name);
    }

    protected @Nullable World createOrLoadWorld(final String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            final WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidGenerator());
            world = wc.createWorld();
        }
        return world;
    }

    private Location pasteSchematic(final World world, final Schematic schematic, final File mapFile, final Arena arena) throws Exception {
        final Location pasteLocation = new Location(
                world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
        schematic.paste(world, pasteLocation, mapFile);
        return pasteLocation;
    }

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
}