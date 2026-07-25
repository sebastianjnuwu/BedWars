package dev.sebastianjnuwu.bedwars.command.admin.config;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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
        arena.setArenaSpawn(loc);
        final var spawnBlock = loc.getBlock().getRelative(0, -1, 0);
        arena.setSpawnBlockData(spawnBlock.getBlockData().getAsString());
        spawnBlock.setType(Material.EMERALD_BLOCK);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.spawn_success",
                arena.getName(),
                String.valueOf(loc.getBlockX()),
                String.valueOf(loc.getBlockY()),
                String.valueOf(loc.getBlockZ())));
    }
}
