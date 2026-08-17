package dev.sebastianjnuwu.bedwars.manager.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Restaura as camas das equipes após a reconstrução do mundo.
 * <p>
 * Como a cama pode ficar fora dos limites do schematic, ela é recolocada
 * programaticamente a partir da configuração da arena (local + direção).
 * </p>
 */
final class ArenaBedRestorer {

    private ArenaBedRestorer() {
    }

    /**
     * Restaura as camas de todas as equipes da arena no mundo.
     *
     * @param world mundo de partida (não nulo)
     * @param arena arena com as configurações de cama (não nula)
     */
    static void restore(final World world, final Arena arena) {
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getBed() == null || team.getBedFacing() == null) {
                continue;
            }
            final BlockFace face;
            try {
                face = BlockFace.valueOf(team.getBedFacing().toUpperCase());
            } catch (final IllegalArgumentException ignored) {
                continue;
            }
            final Material material = bedMaterial(team.getColor());
            final Location foot = new Location(world, team.getBed().getBlockX(), team.getBed().getBlockY(), team.getBed().getBlockZ());
            final Bed footData = (Bed) Bukkit.createBlockData(material);
            footData.setFacing(face);
            footData.setPart(Bed.Part.FOOT);
            foot.getBlock().setBlockData(footData, false);
            final Bed headData = (Bed) Bukkit.createBlockData(material);
            headData.setFacing(face);
            headData.setPart(Bed.Part.HEAD);
            final Location head = foot.clone().add(face.getModX(), face.getModY(), face.getModZ());
            head.getBlock().setBlockData(headData, false);
        }
    }

    private static Material bedMaterial(final String dyeColor) {
        if (dyeColor == null) {
            return Material.RED_BED;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_BED;
            case "BLUE", "AZUL" -> Material.BLUE_BED;
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
            default -> Material.RED_BED;
        };
    }
}
