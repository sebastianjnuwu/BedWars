package dev.sebastianjnuwu.bedwars.command.admin.team;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SetSpawnCommand extends BaseCommand implements ArenaSubCommand {

    public SetSpawnCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setspawn_usage"));
            return;
        }
        final String colorName = args[3].toLowerCase();
        final ArenaTeam team = arena.getTeam(colorName);
        if (team == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setspawn_notfound", colorName));
            return;
        }
        final Location loc = player.getLocation();
        team.setSpawn(loc);
        final var spawnBlock = loc.getBlock().getRelative(0, -1, 0);
        team.setSpawnBlockData(spawnBlock.getBlockData().getAsString());
        spawnBlock.setType(Material.WHITE_WOOL);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.setspawn_success", colorName));
    }
}
