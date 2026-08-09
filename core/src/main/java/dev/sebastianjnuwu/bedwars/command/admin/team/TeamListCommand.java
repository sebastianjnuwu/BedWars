package dev.sebastianjnuwu.bedwars.command.admin.team;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

public class TeamListCommand extends BaseCommand implements ArenaSubCommand {

    public TeamListCommand(
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
        final var teams = arena.getTeams();
        if (teams.isEmpty()) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.YELLOW, "admin.arena.teams_none"));
            return;
        }
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GOLD, "admin.arena.teams_header", arena.getName()));
        for (final ArenaTeam team : teams) {
            final String spawnStatus = team.getSpawn() != null
                    ? this.lang.raw("admin.arena.teams_spawn_ok")
                    : this.lang.raw("admin.arena.teams_spawn_missing");
            final String bedStatus = team.getBed() != null
                    ? this.lang.raw("admin.arena.teams_bed_ok")
                    : this.lang.raw("admin.arena.teams_bed_missing");
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GRAY, "admin.arena.teams_entry",
                    team.getName(), spawnStatus, bedStatus));
        }
    }
}
