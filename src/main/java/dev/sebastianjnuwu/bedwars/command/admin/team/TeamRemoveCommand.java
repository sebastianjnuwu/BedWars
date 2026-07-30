package dev.sebastianjnuwu.bedwars.command.admin.team;

import java.io.File;

import org.bukkit.Location;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/** Removes a configured team and its placed bed from an arena being edited. */
public class TeamRemoveCommand extends BaseCommand implements ArenaSubCommand {

    public TeamRemoveCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.removeteam_usage"));
            return;
        }

        final String teamName = args[3].toLowerCase();
        final ArenaTeam team = arena.getTeam(teamName);
        if (team == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.removeteam_notfound", teamName));
            return;
        }

        this.removeBed(team);
        arena.removeTeam(teamName);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.removeteam_success", teamName));
    }

    private void removeBed(final ArenaTeam team) {
        final Location head = team.getBed();
        if (head == null || !(head.getBlock().getBlockData() instanceof Bed bed)) {
            return;
        }

        head.getBlock().setType(org.bukkit.Material.AIR, false);
        final Location foot = head.clone().subtract(bed.getFacing().getDirection());
        if (foot.getBlock().getBlockData() instanceof Bed) {
            foot.getBlock().setType(org.bukkit.Material.AIR, false);
        }
    }
}
