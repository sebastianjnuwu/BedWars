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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class TeamAddCommand extends BaseCommand implements ArenaSubCommand {

    private static final List<String> TEAM_COLORS = List.of(
            "azul", "vermelho", "verde", "amarelo", "roxo", "rosa", "laranja", "ciano"
    );

    public TeamAddCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addteam_usage"));
            player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "admin.arena.addteam_colors",
                    String.join(", ", TEAM_COLORS)));
            return;
        }
        final String colorName = args[3].toLowerCase();
        if (!TEAM_COLORS.contains(colorName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addteam_invalid",
                    String.join(", ", TEAM_COLORS)));
            return;
        }
        if (arena.getTeam(colorName) != null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addteam_exists", colorName));
            return;
        }
        final String dyeColor = switch (colorName) {
            case "azul" -> "BLUE";
            case "vermelho" -> "RED";
            case "verde" -> "GREEN";
            case "amarelo" -> "YELLOW";
            case "roxo" -> "PURPLE";
            case "rosa" -> "PINK";
            case "laranja" -> "ORANGE";
            case "ciano" -> "CYAN";
            default -> "WHITE";
        };
        arena.addTeam(new dev.sebastianjnuwu.bedwars.model.ArenaTeam(colorName, dyeColor));
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.addteam_success", colorName));
    }
}
