package dev.sebastianjnuwu.bedwars.command.admin.npc;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.npc.ArenaNpc;
import dev.sebastianjnuwu.bedwars.npc.FancyNpcsHook;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;

public class SetShopCommand extends BaseCommand implements ArenaSubCommand {

    public SetShopCommand(
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
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Uso: /bw admin arena <arena> setshop <items|upgrades> [skin]"));
            return;
        }
        final String type = args[3].toLowerCase();
        if (!type.equals("items") && !type.equals("upgrades")) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Tipo invalido. Use items ou upgrades."));
            return;
        }

        final String id = "bw_" + arena.getName() + "_" + type;
        final Location loc = player.getLocation();
        final String skin = args.length > 4 ? args[4] : null;
        final String displayName = type.equals("items")
                ? "<green>Loja de Itens</green>"
                : "<gold>Loja de Upgrades</gold>";

        // Remove existing NPC with same id
        FancyNpcsHook.removeNpc(id);
        arena.getNpcs().removeIf(n -> n.getId().equals(id));

        // Create FancyNpcs NPC
        final Object fNpc = FancyNpcsHook.createNpc(id, loc, skin, displayName);
        if (fNpc == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "FancyNpcs nao esta disponivel."));
            return;
        }

        // Save to arena data
        final ArenaNpc arenaNpc = new ArenaNpc(id);
        arenaNpc.setType(type.toUpperCase());
        arenaNpc.setSkin(skin);
        arenaNpc.setDisplayName(displayName);
        arenaNpc.setLocation(loc);
        arena.getNpcs().add(arenaNpc);

        this.arenaManager.save(arena);
        this.arenaManager.flush(arena.getName());
        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "NPC de " + type + " adicionado!"));
    }
}
