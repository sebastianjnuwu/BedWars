package dev.sebastianjnuwu.bedwars.command.admin.generator;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

public class GeneratorRemoveCommand extends BaseCommand implements ArenaSubCommand {

    public GeneratorRemoveCommand(
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
        final Player player = (Player) sender;
        if (args.length < 4) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.removegen_usage"));
            return;
        }

        final List<ArenaGenerator> gens = arena.getGenerators();

        // Try to find by UUID first
        try {
            final UUID targetId = UUID.fromString(args[3]);
            final ArenaGenerator found = gens.stream()
                    .filter(g -> g.getUniqueId().equals(targetId))
                    .findFirst().orElse(null);
            if (found != null) {
                removeGenerator(player, arena, found);
                return;
            }
        } catch (final IllegalArgumentException ignored) {
            // Not a UUID, try other lookup methods
        }

        // Try to find by team name (for forges)
        final String teamName = args[3].toLowerCase();
        final ArenaGenerator found = gens.stream()
                .filter(g -> g.getType().equalsIgnoreCase("forge")
                        && g.getTeam() != null
                        && g.getTeam().equalsIgnoreCase(teamName))
                .findFirst().orElse(null);
        if (found != null) {
            removeGenerator(player, arena, found);
            return;
        }

        // Try to find by type
        final String rawType = args[3].toLowerCase();
        final String type = switch (rawType) {
            case "ferro" -> "iron";
            case "ouro" -> "gold";
            case "diamante" -> "diamond";
            case "esmeralda" -> "emerald";
            case "fornalha", "forja" -> "forge";
            default -> rawType;
        };
        final ArenaGenerator byType = gens.stream()
                .filter(g -> g.getType().equalsIgnoreCase(type))
                .findFirst().orElse(null);
        if (byType != null) {
            removeGenerator(player, arena, byType);
            return;
        }

        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.removegen_notfound", args[3]));
    }

    private void removeGenerator(final Player player, final Arena arena, final ArenaGenerator gen) {
        final String genInfo = gen.getType()
                + (gen.getTeam() != null ? " (" + gen.getTeam() + ")" : "")
                + " [" + gen.getUniqueId().toString().substring(0, 8) + "...]";

        if (gen.getLocation() != null) {
            final Block markerBlock = gen.getLocation().getBlock();
            if (gen.getOriginBlockData() != null) {
                markerBlock.setBlockData(org.bukkit.Bukkit.createBlockData(gen.getOriginBlockData()), false);
            } else {
                markerBlock.setType(Material.AIR, false);
            }
        }

        arena.getGenerators().remove(gen);
        this.arenaManager.save(arena);
        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "admin.arena.removegen_success", genInfo));
    }
}
