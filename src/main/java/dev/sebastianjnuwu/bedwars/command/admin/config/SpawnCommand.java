package dev.sebastianjnuwu.bedwars.command.admin.config;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

public class SpawnCommand extends BaseCommand implements ArenaSubCommand {

    public SpawnCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
    }

    @Override
    public void execute(final CommandSender sender, final @NotNull Arena arena, final @NotNull String @NotNull [] args) {
        final Player player = (Player) sender;
        final Location loc = player.getLocation();
        final var newSpawnBlock = loc.getBlock().getRelative(0, -1, 0);

        if (arena.getArenaSpawn() != null && arena.getSpawnBlockData() != null) {
            try {
                final var oldBlock = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0);
                oldBlock.setBlockData(org.bukkit.Bukkit.createBlockData(arena.getSpawnBlockData()), false);
            } catch (final Exception ignored) {
            }
        }

        arena.setArenaSpawn(loc);
        arena.setSpawnBlockData(newSpawnBlock.getBlockData().getAsString());
        newSpawnBlock.setType(Material.EMERALD_BLOCK, false);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.spawn_success",
                arena.getName(),
                String.valueOf(loc.getBlockX()),
                String.valueOf(loc.getBlockY()),
                String.valueOf(loc.getBlockZ())));
    }
}
