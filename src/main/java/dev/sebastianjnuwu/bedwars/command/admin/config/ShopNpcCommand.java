package dev.sebastianjnuwu.bedwars.command.admin.config;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

/**
 * Comando para gerenciar NPCs da loja em uma arena.
 * <p>
 * Subcomandos:
 * <ul>
 *   <li><b>add [skin]</b> — adiciona um NPC na posição atual do jogador</li>
 *   <li><b>remove &lt;id&gt;</b> — remove um NPC pelo índice</li>
 *   <li><b>list</b> — lista todos os NPCs configurados</li>
 * </ul>
 */
public class ShopNpcCommand extends BaseCommand implements ArenaSubCommand {

    public ShopNpcCommand(
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
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.setlobby.only_player"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Uso: /bw admin arena <arena> shop-npc add <skin>"));
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Uso: /bw admin arena <arena> shop-npc remove <id>"));
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "Uso: /bw admin arena <arena> shop-npc list"));
            return;
        }

        String action = args[3].toLowerCase();

        switch (action) {
            case "add" -> {
                String skin = args.length > 4 ? args[4] : "NPC";
                Location loc = player.getLocation();
                var locations = new ArrayList<>(arena.getShopNpcLocations());
                locations.add(loc);
                arena.setShopNpcLocations(locations);
                arena.setShopNpcSkin(skin);
                this.arenaManager.save(arena);
                sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "Shop NPC adicionado na arena " + arena.getName()));
            }
            case "remove" -> {
                var locations = new ArrayList<>(arena.getShopNpcLocations());
                if (args.length > 4) {
                    try {
                        int id = Integer.parseInt(args[4]);
                        if (id >= 0 && id < locations.size()) {
                            locations.remove(id);
                            arena.setShopNpcLocations(locations);
                            this.arenaManager.save(arena);
                            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "Shop NPC " + id + " removido"));
                        } else {
                            sender.sendMessage(this.lang.text(NamedTextColor.RED, "ID invalido"));
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(this.lang.text(NamedTextColor.RED, "ID invalido"));
                    }
                } else if (!locations.isEmpty()) {
                    locations.remove(locations.size() - 1);
                    arena.setShopNpcLocations(locations);
                    this.arenaManager.save(arena);
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "Ultimo Shop NPC removido"));
                }
            }
            case "list" -> {
                var locations = arena.getShopNpcLocations();
                if (locations.isEmpty()) {
                    sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "Nenhum Shop NPC configurado"));
                } else {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "Shop NPCs da arena " + arena.getName() + ":"));
                    for (int i = 0; i < locations.size(); i++) {
                        Location l = locations.get(i);
                        sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "  " + i + ": " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ()));
                    }
                    sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "Skin: " + arena.getShopNpcSkin()));
                }
            }
            default -> sender.sendMessage(this.lang.text(NamedTextColor.RED, "Acao invalida. Use: add, remove, list"));
        }
    }
}
