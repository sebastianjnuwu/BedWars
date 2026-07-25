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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class SetBedCommand extends BaseCommand implements ArenaSubCommand {

    public SetBedCommand(
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
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setbed_usage"));
            return;
        }
        final String colorName = args[3].toLowerCase();
        final ArenaTeam team = arena.getTeam(colorName);
        if (team == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setbed_notfound", colorName));
            return;
        }
        final Location loc = player.getLocation();
        final BlockFace facing = this.yawToFace(player.getYaw());
        final Location headLoc = loc.clone().add(facing.getDirection());
        final Location footLoc = loc;
        team.setBed(headLoc);
        team.setBedFacing(facing.name());
        final Material bedMaterial = this.getBedMaterial(team.getColor());
        headLoc.getBlock().setType(bedMaterial);
        final Bed headData = (Bed) headLoc.getBlock().getBlockData();
        headData.setFacing(facing);
        headData.setPart(Bed.Part.HEAD);
        headLoc.getBlock().setBlockData(headData, false);
        footLoc.getBlock().setType(bedMaterial);
        final Bed footData = (Bed) footLoc.getBlock().getBlockData();
        footData.setFacing(facing);
        footData.setPart(Bed.Part.FOOT);
        footLoc.getBlock().setBlockData(footData, false);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.setbed_success", colorName, facing.name()));
    }

    private Material getBedMaterial(final String dyeColor) {
        return switch (dyeColor.toUpperCase()) {
            case "BLUE" -> Material.BLUE_BED;
            case "RED" -> Material.RED_BED;
            case "GREEN" -> Material.GREEN_BED;
            case "YELLOW" -> Material.YELLOW_BED;
            case "PURPLE" -> Material.PURPLE_BED;
            case "PINK" -> Material.PINK_BED;
            case "ORANGE" -> Material.ORANGE_BED;
            case "CYAN" -> Material.CYAN_BED;
            default -> Material.WHITE_BED;
        };
    }

    private BlockFace yawToFace(final float yaw) {
        final float dir = (yaw % 360 + 360) % 360;
        if (dir < 45 || dir >= 315) return BlockFace.SOUTH;
        if (dir < 135) return BlockFace.WEST;
        if (dir < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }
}
