package dev.sebastianjnuwu.bedwars.command.admin.team;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.arena.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

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
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.setbed_usage"));
            return;
        }
        final String colorName = args[3].toLowerCase();
        final ArenaTeam team = arena.getTeam(colorName);
        if (team == null) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.setbed_notfound", colorName));
            return;
        }

        final BlockFace facing = this.yawToFace(player.getYaw());
        final Material bedMaterial = this.getBedMaterial(team.getColor());

        Block targetBlock = player.getTargetBlockExact(5);

        if (targetBlock == null || !(targetBlock.getBlockData() instanceof Bed)) {
            targetBlock = this.findNearbyBed(player.getLocation(), 5);
        }

        if (targetBlock != null && targetBlock.getBlockData() instanceof Bed) {
            this.recolorBed(targetBlock, bedMaterial);
            final Location bedLoc = this.getBedFootLocation(targetBlock);
            team.setBed(bedLoc);
            final BlockFace bedFacing = this.getBedFacing(targetBlock);
            team.setBedFacing(bedFacing != null ? bedFacing.name() : facing.name());
            this.arenaManager.save(arena);
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "admin.arena.setbed_success",
                    colorName, team.getBedFacing()));
        } else {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "admin.arena.setbed_stand_on_bed"));
        }
    }

    /**
     * Procura blocos próximos por qualquer bloco de cama (FOOT ou HEAD).
     */
    private Block findNearbyBed(final Location center, final int radius) {
        final int cx = center.getBlockX();
        final int cy = center.getBlockY();
        final int cz = center.getBlockZ();
        Block closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = cy - radius; y <= cy + radius; y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    final Block block = center.getWorld().getBlockAt(x, y, z);
                    if (block.getBlockData() instanceof Bed) {
                        final double dist = block.getLocation().distanceSquared(center);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = block;
                        }
                    }
                }
            }
        }
        return closest;
    }

    /**
     * Recolore uma cama existente (ambas as metades) para o material de cama alvo.
     */
    private void recolorBed(final Block bedBlock, final Material bedMaterial) {
        final Bed bedData = (Bed) bedBlock.getBlockData();
        final Block otherHalf;

        if (bedData.getPart() == Bed.Part.HEAD) {
            otherHalf = bedBlock.getRelative(bedData.getFacing().getOppositeFace());
        } else {
            otherHalf = bedBlock.getRelative(bedData.getFacing());
        }

        // Recolor head
        final Block headBlock = bedData.getPart() == Bed.Part.HEAD ? bedBlock : otherHalf;
        final Block footBlock = bedData.getPart() == Bed.Part.FOOT ? bedBlock : otherHalf;

        final BlockFace bedFacing = bedData.getFacing();

        // Set foot first, then head
        footBlock.setType(bedMaterial, false);
        final Bed footBd = (Bed) Bukkit.createBlockData(bedMaterial);
        footBd.setFacing(bedFacing);
        footBd.setPart(Bed.Part.FOOT);
        footBlock.setBlockData(footBd, false);

        headBlock.setType(bedMaterial, false);
        final Bed headBd = (Bed) Bukkit.createBlockData(bedMaterial);
        headBd.setFacing(bedFacing);
        headBd.setPart(Bed.Part.HEAD);
        headBlock.setBlockData(headBd, false);
    }

    /**
     * Obtém a direção para a qual uma cama está virada.
     */
    private BlockFace getBedFacing(final Block bedBlock) {
        if (bedBlock.getBlockData() instanceof final Bed bed) {
            return bed.getFacing();
        }
        return null;
    }

    private Location getBedFootLocation(final Block bedBlock) {
        if (!(bedBlock.getBlockData() instanceof final Bed bed)) {
            return bedBlock.getLocation();
        }
        if (bed.getPart() == Bed.Part.FOOT) {
            return bedBlock.getLocation();
        }
        return bedBlock.getRelative(bed.getFacing().getOppositeFace()).getLocation();
    }

    private Material getBedMaterial(final String dyeColor) {
        if (dyeColor == null) {
            return Material.WHITE_BED;
        }
        return switch (dyeColor.toUpperCase()) {
            case "BLUE", "AZUL" -> Material.BLUE_BED;
            case "RED", "VERMELHO" -> Material.RED_BED;
            case "GREEN", "VERDE" -> Material.GREEN_BED;
            case "YELLOW", "AMARELO" -> Material.YELLOW_BED;
            case "PURPLE", "ROXO" -> Material.PURPLE_BED;
            case "PINK", "ROSA" -> Material.PINK_BED;
            case "ORANGE", "LARANJA" -> Material.ORANGE_BED;
            case "CYAN", "CIANO" -> Material.CYAN_BED;
            case "LIME", "VERDE_LIMA" -> Material.LIME_BED;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_BED;
            case "GRAY", "CINZA" -> Material.GRAY_BED;
            case "BLACK", "PRETO" -> Material.BLACK_BED;
            case "WHITE", "BRANCO" -> Material.WHITE_BED;
            default -> Material.WHITE_BED;
        };
    }

    private BlockFace yawToFace(final float yaw) {
        final float dir = (yaw % 360 + 360) % 360;
        if (dir < 45 || dir >= 315) {
            return BlockFace.SOUTH;
        }
        if (dir < 135) {
            return BlockFace.WEST;
        }
        if (dir < 225) {
            return BlockFace.NORTH;
        }
        return BlockFace.EAST;
    }
}
