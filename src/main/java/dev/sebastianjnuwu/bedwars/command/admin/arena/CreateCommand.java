package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;

public class CreateCommand extends BaseCommand implements SubCommand {

    public CreateCommand(
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

        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.usage"));
            return;
        }

        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }

        final String name = args[1];

        if (this.arenaManager.get(name) != null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.already_exists", name));
            return;
        }

        if (!this.configManager.hasLobby()) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "no_lobby"));
            return;
        }

        final WorldEditPlugin worldEdit
                = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEdit == null) {
            sender.sendMessage(this.lang.text(
                    NamedTextColor.RED,
                    "create.error",
                    "WorldEdit nao encontrado."
            ));
            return;
        }

        try {
            final LocalSession session = worldEdit.getSession(player);

            final var selection
                    = session.getSelection(BukkitAdapter.adapt(player.getWorld()));

            final BlockVector3 min = selection.getMinimumPoint();
            final BlockVector3 max = selection.getMaximumPoint();

            final int width = max.x() - min.x() + 1;
            final int height = max.y() - min.y() + 1;
            final int length = max.z() - min.z() + 1;

            final int maxWidth
                    = this.configManager.getConfig().getInt("arena.limits.max-width");

            final int maxHeight
                    = this.configManager.getConfig().getInt("arena.limits.max-height");

            final int maxLength
                    = this.configManager.getConfig().getInt("arena.limits.max-length");

            if ((maxWidth > 0 && width > maxWidth)
                    || (maxHeight > 0 && height > maxHeight)
                    || (maxLength > 0 && length > maxLength)) {

                sender.sendMessage(this.lang.text(
                        NamedTextColor.RED,
                        "create.too_large",
                        width + "x" + height + "x" + length
                ));
                return;
            }

            final Arena arena = this.arenaManager.create(name);

            if (arena == null) {
                sender.sendMessage(this.lang.text(
                        NamedTextColor.RED,
                        "create.already_exists",
                        name
                ));
                return;
            }

            arena.setPaste(
                    min.x(),
                    min.y(),
                    min.z()
            );

            arena.setSchematicSize(
                    width,
                    height,
                    length
            );

            arena.setWorldName(
                    player.getWorld().getName()
            );

            arena.setEnabled(true);

            final org.bukkit.Location pos1 = new org.bukkit.Location(
                    player.getWorld(), min.x(), min.y(), min.z());
            final org.bukkit.Location pos2 = new org.bukkit.Location(
                    player.getWorld(), max.x(), max.y(), max.z());
            final dev.sebastianjnuwu.bedwars.world.Schematic schematic =
                    new dev.sebastianjnuwu.bedwars.world.Schematic(name, pos1, pos2);

            File schematicFile = new File(this.mapsFolder, name + ".schem");
            try {
                schematic.save(schematicFile);
            } catch (final Exception e) {
                schematicFile = new File(this.mapsFolder, name + ".bwmap");
                try {
                    schematic.save(schematicFile);
                } catch (final Exception ignored) {
                }
            }

            this.arenaManager.save(arena);

            sender.sendMessage(this.lang.text(
                    NamedTextColor.GREEN,
                    "create.success",
                    name,
                    String.valueOf(width * height * length)
            ));

        } catch (final IncompleteRegionException e) {

            sender.sendMessage(
                    this.lang.text(
                            NamedTextColor.RED,
                            "create.no_selection"
                    )
            );
        }
    }
}
