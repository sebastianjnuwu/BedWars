package dev.sebastianjnuwu.bedwars.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class WorldManager {

    private static final Set<String> IGNORED_FILES = new HashSet<>(Arrays.asList(
            "uid.dat", "session.dat", "session.lock"
    ));

    private final JavaPlugin plugin;
    private final File templatesFolder;

    public WorldManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.templatesFolder = new File(plugin.getDataFolder(), "templates");
        this.templatesFolder.mkdirs();
    }

    public File getTemplatesFolder() {
        return this.templatesFolder;
    }

    public File getTemplateFolder(final String name) {
        return new File(this.templatesFolder, name);
    }

    public boolean templateExists(final String name) {
        return getTemplateFolder(name).isDirectory();
    }

    public void saveTemplate(final String name, final World source) throws IOException {
        final File dest = getTemplateFolder(name);
        deleteWorldFile(dest);
        dest.mkdirs();
        copyWorld(source.getWorldFolder(), dest);
    }

    public World copyAndLoadTemplate(final String name, final String targetName) throws IOException {
        final File template = getTemplateFolder(name);
        if (!template.isDirectory()) {
            throw new IOException("Template nao encontrado: " + name);
        }

        final File targetFolder = new File(Bukkit.getWorldContainer(), targetName);
        deleteWorldFile(targetFolder);

        copyWorld(template, targetFolder);

        final World world = loadWorld(targetName);
        return world;
    }

    public World loadWorld(final String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            return world;
        }

        final WorldCreator wc = new WorldCreator(name);
        wc.generateStructures(false);
        world = wc.createWorld();

        if (world != null) {
            world.setDifficulty(org.bukkit.Difficulty.NORMAL);
            world.setSpawnFlags(true, true);
            world.setPVP(true);
            world.setStorm(false);
            world.setThundering(false);
            world.setWeatherDuration(Integer.MAX_VALUE);
            world.setKeepSpawnInMemory(false);
            world.setTicksPerAnimalSpawns(1);
            world.setTicksPerMonsterSpawns(1);
            world.setAutoSave(false);

            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, true);
            world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(org.bukkit.GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setGameRule(org.bukkit.GameRule.SHOW_DEATH_MESSAGES, false);
        }

        return world;
    }

    public boolean unloadWorld(final String name) {
        final World world = Bukkit.getWorld(name);
        if (world == null) return false;

        for (final Player p : world.getPlayers()) {
            p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        return Bukkit.unloadWorld(world, false);
    }

    public void deleteWorld(final String name) {
        unloadWorld(name);
        final File folder = new File(Bukkit.getWorldContainer(), name);
        deleteWorldFile(folder);
    }

    private void deleteWorldFile(final File path) {
        if (!path.exists()) return;
        final File[] files = path.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteWorldFile(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }

    private void copyWorld(final File source, final File target) throws IOException {
        if (IGNORED_FILES.contains(source.getName())) return;

        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) {
                throw new IOException("Nao foi possivel criar diretorio: " + target);
            }
            final File[] files = source.listFiles();
            if (files != null) {
                for (final File file : files) {
                    copyWorld(file, new File(target, file.getName()));
                }
            }
        } else {
            try (final InputStream in = new FileInputStream(source);
                 final OutputStream out = new FileOutputStream(target)) {
                final byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        }
    }

    public void deleteAllBwWorlds() {
        final Set<String> toDelete = new HashSet<>();
        for (final World world : Bukkit.getWorlds()) {
            if (world.getName().startsWith("bw_")) {
                toDelete.add(world.getName());
            }
        }
        for (final String name : toDelete) {
            deleteWorld(name);
        }
    }
}
