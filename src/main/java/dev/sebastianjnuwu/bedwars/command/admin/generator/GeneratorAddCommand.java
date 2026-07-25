package dev.sebastianjnuwu.bedwars.command.admin.generator;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class GeneratorAddCommand extends BaseCommand implements ArenaSubCommand {

    public GeneratorAddCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_usage"));
            return;
        }
        final String rawType = args[3].toLowerCase();
        final String type = switch (rawType) {
            case "ferro" -> "iron";
            case "ouro" -> "gold";
            case "diamante", "diamond" -> "diamond";
            case "esmeralda", "emerald" -> "emerald";
            default -> rawType;
        };
        if (!List.of("iron", "gold", "diamond", "emerald", "forge").contains(type)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_invalid"));
            return;
        }
        final Location loc = player.getLocation();
        for (final var gen : arena.getGenerators()) {
            if (gen.getLocation().equals(loc)) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_duplicate"));
                return;
            }
        }
        final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(type, loc);
        final var below = loc.getBlock().getRelative(0, -1, 0);
        if (gen.getOriginBlockData() == null) {
            gen.setOriginBlockData(below.getBlockData().getAsString());
        }
        // Marker block for generator in edit mode
        final Material marker = switch (type) {
            case "diamond" -> Material.DIAMOND_BLOCK;
            case "emerald" -> Material.EMERALD_BLOCK;
            case "gold" -> Material.GOLD_BLOCK;
            case "iron" -> Material.IRON_BLOCK;
            default -> Material.SPONGE;
        };
        below.setType(marker);

        arena.addGenerator(gen);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.addgen_success", type));
    }
}
