package dev.sebastianjnuwu.bedwars.command.admin;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * /bw admin reload
 *
 * Reloads all arena .yml files from disk and the lang file.
 * Safe to use while the server is running — does not affect
 * active games or open editor sessions.
 */
public class ReloadCommand extends BaseCommand implements SubCommand {

    public ReloadCommand(
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
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        // Reload all arena YMLs
        this.arenaManager.load();

        // Reload lang messages
        this.lang.load();

        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.reload_success",
                String.valueOf(this.arenaManager.getNames().size())));
    }
}
