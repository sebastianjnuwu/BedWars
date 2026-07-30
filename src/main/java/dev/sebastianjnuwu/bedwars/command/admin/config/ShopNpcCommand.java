package dev.sebastianjnuwu.bedwars.command.admin.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;

import de.oliver.fancynpcs.api.Npc;

/**
 * Comando para gerenciar NPCs da loja em uma arena.
 * <p>
 * Subcomandos:
 * <ul>
 *   <li><b>add [skin]</b> — adiciona um NPC na posicao atual do jogador</li>
 *   <li><b>remove &lt;id&gt;</b> — remove um NPC pelo indice</li>
 *   <li><b>list</b> — lista todos os NPCs configurados</li>
 * </ul>
 */
public class ShopNpcCommand extends BaseCommand implements ArenaSubCommand {

    private final ShopNpcManager shopNpcManager;

    public ShopNpcCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.shopNpcManager = gameManager.getShopNpcManager();
    }

    @Override
    public void execute(final CommandSender sender, final @NotNull Arena arena, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.setlobby.only_player"));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.usage_add"));
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.usage_remove"));
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.usage_list"));
            return;
        }

        final String action = args[3].toLowerCase();

        switch (action) {
            case "add" -> {
                final String skin = args.length > 4 ? args[4] : "NPC";
                final Location loc = player.getLocation();
                final List<Location> locations = new ArrayList<>(arena.getShopNpcLocations());
                final int index = locations.size();
                locations.add(loc);
                arena.setShopNpcLocations(locations);
                arena.setShopNpcSkin(skin);
                this.arenaManager.save(arena);

                final Npc npc = this.shopNpcManager.spawnSingleNpc(arena.getName(), index, loc, skin);
                if (npc != null) {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.added", arena.getName()));
                } else {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.added_no_npcs"));
                }
            }
            case "remove" -> {
                final List<Location> locations = new ArrayList<>(arena.getShopNpcLocations());
                if (args.length > 4) {
                    try {
                        final int id = Integer.parseInt(args[4]);
                        if (id >= 0 && id < locations.size()) {
                            locations.remove(id);
                            arena.setShopNpcLocations(locations);
                            this.arenaManager.save(arena);
                            this.shopNpcManager.removeEditorNpcs(arena.getName());
                            this.shopNpcManager.spawnEditorNpcs(arena.getName(), arena.getShopNpcLocations(), arena.getShopNpcSkin());
                            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.removed", id));
                        } else {
                            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                        }
                    } catch (final NumberFormatException e) {
                        sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                    }
                } else if (!locations.isEmpty()) {
                    locations.remove(locations.size() - 1);
                    arena.setShopNpcLocations(locations);
                    this.arenaManager.save(arena);
                    this.shopNpcManager.removeEditorNpcs(arena.getName());
                    this.shopNpcManager.spawnEditorNpcs(arena.getName(), arena.getShopNpcLocations(), arena.getShopNpcSkin());
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.removed_last"));
                }
            }
            case "list" -> {
                final List<Location> locations = arena.getShopNpcLocations();
                if (locations.isEmpty()) {
                    sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "admin.arena.shop_npc.none"));
                } else {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.header", arena.getName()));
                    for (int i = 0; i < locations.size(); i++) {
                        final Location l = locations.get(i);
                        sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.shop_npc.entry", i, l.getBlockX(), l.getBlockY(), l.getBlockZ()));
                    }
                    sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.shop_npc.skin_label", arena.getShopNpcSkin()));
                }
            }
            default -> sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.unknown_action"));
        }
    }
}
