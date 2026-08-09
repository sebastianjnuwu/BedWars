package dev.sebastianjnuwu.bedwars.command.admin;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.command.admin.arena.LifecycleRouter;
import dev.sebastianjnuwu.bedwars.command.admin.arena.SetLobbyCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando administrativo do BedWars (<b>/bw admin</b>).
 */
public class AdminCommand extends BaseCommand {

    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    /**
     * Construtor que inicializa e registra todos os subcomandos administrativos.
     */
    public AdminCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);

        final LifecycleRouter lifecycle = new LifecycleRouter(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.register("arena", new ArenaRouter(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setlobby", new SetLobbyCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("create", lifecycle);
        this.register("delete", lifecycle);
        this.register("list", lifecycle);
        this.register("save", lifecycle);
        this.register("load", lifecycle);
        this.register("edit", lifecycle);
        this.register("discard", lifecycle);
        this.register("reload", new ReloadCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
    }

    /**
     * Executa o comando administrativo.
     */
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "admin.usage"));
            return;
        }
        final SubCommand cmd = this.subcommands.get(args[1].toLowerCase());
        if (cmd == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "admin.unknown", args[1]));
            return;
        }
        cmd.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Registra um subcomando administrativo no mapa interno.
     */
    private void register(final String name, final SubCommand cmd) {
        this.subcommands.put(name, cmd);
    }
}
