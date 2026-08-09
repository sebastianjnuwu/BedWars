package dev.sebastianjnuwu.bedwars.command.admin;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * /bw admin reload
 *
 * Recarrega todos os arquivos .yml de arena do disco e o arquivo de idioma.
 * Seguro para usar enquanto o servidor está em execução — não afeta
 * partidas ativas ou sessões de edição abertas.
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
        // Reload all arena YMLs directly from disk (sem flush: o flush sobrescreveria
        // edições manuais feitas nos .yml, como spawn_item)
        this.arenaManager.load();

        // Reload lang messages
        this.lang.load();

        // Reload shop items (invalidate cache so GUIs rebuild from disk)
        final BedWarsPlugin plugin = JavaPlugin.getPlugin(BedWarsPlugin.class);
        plugin.getShopManager().invalidateAll();
        plugin.getShopManager().loadDefaults();

        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "admin.reload_success",
                String.valueOf(this.arenaManager.getNames().size())));
    }
}
