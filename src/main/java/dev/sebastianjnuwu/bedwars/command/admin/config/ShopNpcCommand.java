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
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopNpcManager;

/**
 * Comando para gerenciar NPCs da loja em uma arena.
 * <p>
 * Subcomandos:
 * <ul>
 *   <li><b>add [skin] [displayName]</b> — adiciona um NPC na posicao atual do jogador</li>
 *   <li><b>remove &lt;id&gt;</b> — remove um NPC pelo indice</li>
 *   <li><b>list</b> — lista todos os NPCs configurados</li>
 *   <li><b>displayName &lt;id&gt; &lt;nome&gt;</b> — define o nome de exibicao de um NPC</li>
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
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.usage_display_name"));
            return;
        }

        final String action = args[3].toLowerCase();

        switch (action) {
            case "add" -> {
                final String skin = args.length > 4 ? args[4] : "NPC";
                final String displayName = args.length > 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 5, args.length)) : null;
                final List<ShopNpc> npcs = new ArrayList<>(arena.getShopNpcs());
                final int index = npcs.size();
                npcs.add(new ShopNpc(player.getLocation(), skin, displayName));
                arena.setShopNpcs(npcs);
                this.arenaManager.save(arena);

                final Object npc = this.shopNpcManager.spawnSingleNpc(arena.getName(), index, npcs.get(index));
                if (npc != null) {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.added", arena.getName()));
                } else {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.added_no_npcs"));
                }
            }
            case "remove" -> {
                final List<ShopNpc> npcs = new ArrayList<>(arena.getShopNpcs());
                if (args.length > 4) {
                    try {
                        final int id = Integer.parseInt(args[4]);
                        if (id >= 0 && id < npcs.size()) {
                            npcs.remove(id);
                            arena.setShopNpcs(npcs);
                            this.arenaManager.save(arena);
                            this.shopNpcManager.removeEditorNpcs(arena.getName());
                            this.shopNpcManager.spawnEditorNpcs(arena.getName(), arena.getShopNpcs());
                            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.removed", id));
                        } else {
                            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                        }
                    } catch (final NumberFormatException e) {
                        sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                    }
                } else if (!npcs.isEmpty()) {
                    npcs.remove(npcs.size() - 1);
                    arena.setShopNpcs(npcs);
                    this.arenaManager.save(arena);
                    this.shopNpcManager.removeEditorNpcs(arena.getName());
                    this.shopNpcManager.spawnEditorNpcs(arena.getName(), arena.getShopNpcs());
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.removed_last"));
                }
            }
            case "list" -> {
                final List<ShopNpc> npcs = arena.getShopNpcs();
                if (npcs.isEmpty()) {
                    sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "admin.arena.shop_npc.none"));
                } else {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.header", arena.getName()));
                    for (int i = 0; i < npcs.size(); i++) {
                        final ShopNpc npc = npcs.get(i);
                        final Location l = npc.location();
                        sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.shop_npc.entry", i, l.getBlockX(), l.getBlockY(), l.getBlockZ()));
                        sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.shop_npc.entry_skin", npc.skin()));
                        sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.shop_npc.entry_display_name",
                                npc.displayName() != null ? npc.displayName() : "<red>Loja</red>"));
                    }
                }
            }
            case "displayname" -> {
                if (args.length < 6) {
                    sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.usage_display_name"));
                    return;
                }
                final List<ShopNpc> npcs = new ArrayList<>(arena.getShopNpcs());
                try {
                    final int id = Integer.parseInt(args[4]);
                    if (id < 0 || id >= npcs.size()) {
                        sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                        return;
                    }
                    final String name = String.join(" ", java.util.Arrays.copyOfRange(args, 5, args.length));
                    final ShopNpc old = npcs.get(id);
                    npcs.set(id, new ShopNpc(old.location(), old.skin(), name));
                    arena.setShopNpcs(npcs);
                    this.arenaManager.save(arena);
                    this.shopNpcManager.removeEditorNpcs(arena.getName());
                    this.shopNpcManager.spawnEditorNpcs(arena.getName(), arena.getShopNpcs());
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.shop_npc.display_name_set", id, name));
                } catch (final NumberFormatException e) {
                    sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.invalid_id"));
                }
            }
            default -> sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.shop_npc.unknown_action"));
        }
    }
}
