package dev.sebastianjnuwu.bedwars.command;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopGui;
import dev.sebastianjnuwu.bedwars.shop.ShopManager;

public class ShopCommand extends BaseCommand implements SubCommand {

    private final ShopManager shopManager;

    public ShopCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder,
            final ShopManager shopManager
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.shopManager = shopManager;
    }

    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "admin.setlobby.only_player"));
            return;
        }

        String shopName = "default";
        if (args.length > 1) {
            shopName = args[1];
        }

        new ShopGui(shopManager, lang, player, null, shopName);
    }
}
