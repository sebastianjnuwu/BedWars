package dev.sebastianjnuwu.bedwars.command.admin.team;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SetSpawnCommand extends BaseCommand implements ArenaSubCommand {

    public SetSpawnCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setspawn_usage"));
            return;
        }
        final String colorName = args[3].toLowerCase();
        final ArenaTeam team = arena.getTeam(colorName);
        if (team == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setspawn_notfound", colorName));
            return;
        }
        final Location loc = player.getLocation();
        final org.bukkit.block.Block newSpawnBlock = loc.getBlock().getRelative(0, -1, 0);

        if (team.getSpawn() != null && team.getSpawnBlockData() != null) {
            try {
                final org.bukkit.block.Block oldBlock = team.getSpawn().getBlock().getRelative(0, -1, 0);
                oldBlock.setBlockData(org.bukkit.Bukkit.createBlockData(team.getSpawnBlockData()), false);
            } catch (final Exception ignored) {
            }
        }

        team.setSpawn(loc);
        team.setSpawnBlockData(newSpawnBlock.getBlockData().getAsString());

        final Material woolMaterial = this.getWoolMaterial(team.getColor());
        newSpawnBlock.setType(woolMaterial, false);

        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.setspawn_success", colorName));
    }

    public static Material getWoolMaterial(final String dyeColor) {
        if (dyeColor == null) return Material.WHITE_WOOL;
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_WOOL;
            case "BLUE", "AZUL" -> Material.BLUE_WOOL;
            case "GREEN", "VERDE" -> Material.GREEN_WOOL;
            case "YELLOW", "AMARELO" -> Material.YELLOW_WOOL;
            case "PURPLE", "ROXO" -> Material.PURPLE_WOOL;
            case "PINK", "ROSA" -> Material.PINK_WOOL;
            case "ORANGE", "LARANJA" -> Material.ORANGE_WOOL;
            case "CYAN", "CIANO" -> Material.CYAN_WOOL;
            case "LIME", "VERDE_LIMA" -> Material.LIME_WOOL;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_WOOL;
            case "GRAY", "CINZA" -> Material.GRAY_WOOL;
            case "BLACK", "PRETO" -> Material.BLACK_WOOL;
            case "WHITE", "BRANCO" -> Material.WHITE_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }
}
