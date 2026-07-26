package dev.sebastianjnuwu.bedwars.command.admin.npc;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.npc.FancyNpcsHook;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.io.File;

public class RemoveShopCommand extends BaseCommand implements ArenaSubCommand {

    public RemoveShopCommand(
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
    public void execute(final CommandSender sender, final Arena arena, final String[] args) {
        if (args.length < 4) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Uso: /bw admin arena <arena> removshop <items|upgrades>"));
            return;
        }
        final String type = args[3].toLowerCase();
        if (!type.equals("items") && !type.equals("upgrades")) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Tipo invalido. Use items ou upgrades."));
            return;
        }

        final String id = "bw_" + arena.getName() + "_" + type;
        FancyNpcsHook.removeNpc(id);
        arena.getNpcs().removeIf(n -> n.getId().equals(id));
        this.arenaManager.save(arena);
        this.arenaManager.flush(arena.getName());
        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "NPC de " + type + " removido!"));
    }
}
