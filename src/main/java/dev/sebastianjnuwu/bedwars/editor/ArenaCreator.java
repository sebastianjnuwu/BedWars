package dev.sebastianjnuwu.bedwars.editor;

import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

/**
 * Cria mundos de edição para arenas.
 * <p>
 * O ArenaCreator cria mundos "Void" com configurações específicas
 * para que administradores possam construir arenas usando FAWE.
 * </p>
 */
public class ArenaCreator {

    private final SlimeManager slimeManager;
    private final File pluginFolder;
    private final File mapsFolder;
    private final File templatesFolder;

    public ArenaCreator(@NotNull SlimeManager slimeManager, @NotNull File pluginFolder, @NotNull File mapsFolder, @NotNull File templatesFolder) {
        this.slimeManager = slimeManager;
        this.pluginFolder = pluginFolder;
        this.mapsFolder = mapsFolder;
        this.templatesFolder = templatesFolder;
    }

    /**
     * Cria um mundo de edição para uma arena.
     *
     * @param arenaName nome da arena
     * @param creator jogador que está criando
     * @return nome do mundo criado ou null se falhar
     */
    public @Nullable String createEditWorld(@NotNull String arenaName, @NotNull Player creator) {
        final String worldName = generateEditWorldName(arenaName);

        // Cria o mundo void usando SlimeWorld
        final var slimeWorld = slimeManager.createVoidSlimeWorld(worldName);
        if (slimeWorld == null) {
            return null;
        }
        final World world = slimeManager.createBukkitWorld(slimeWorld);
        if (world == null) {
            return null;
        }

        // Configurações básicas de segurança para o mundo de edição
        world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        world.setTime(1000);
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);

        // Teleporta o administrador
        teleportEditor(creator, world);

        return worldName;
    }

    /**
     * Teleporta o administrador para o mundo de edição.
     *
     * @param player jogador
     * @param world mundo
     */
    private void teleportEditor(@NotNull Player player, @NotNull World world) {
        final org.bukkit.Location location = new org.bukkit.Location(world, 0.5, 2, 0.5);
        player.teleport(location);
    }

    /**
     * Deleta um mundo de edição.
     *
     * @param worldName nome do mundo
     */
    public void deleteEditWorld(@NotNull String worldName) {
        final World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, true);
        }
    }

    /**
     * Retorna o nome do mundo de edição para uma arena.
     *
     * @param arenaName nome da arena
     * @return nome do mundo
     */
    public @NotNull String getEditWorldName(@NotNull String arenaName) {
        return generateEditWorldName(arenaName);
    }

    /**
     * Gera um nome único para o mundo de edição.
     *
     * @param arenaName nome da arena
     * @return nome do mundo
     */
    private @NotNull String generateEditWorldName(@NotNull String arenaName) {
        return "bw_edit_" + arenaName + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
