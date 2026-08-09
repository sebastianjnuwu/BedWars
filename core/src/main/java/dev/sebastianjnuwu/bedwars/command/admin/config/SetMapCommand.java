package dev.sebastianjnuwu.bedwars.command.admin.config;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

public class SetMapCommand extends BaseCommand implements ArenaSubCommand {

    public SetMapCommand(
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
        if (args.length < 4) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.setmap_usage"));
            return;
        }
        final String mapName = args[3];
        if ("default".equalsIgnoreCase(mapName)) {
            arena.setMapName(null);
            this.arenaManager.save(arena);
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "admin.arena.setmap_reset", arena.getName()));
            return;
        }
        if (this.arenaManager.getMapFile(mapName) == null) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.setmap_not_found", mapName));
            return;
        }
        arena.setMapName(mapName);
        this.arenaManager.save(arena);
        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "admin.arena.setmap_success", mapName));
    }
}
