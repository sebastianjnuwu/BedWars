package dev.sebastianjnuwu.bedwars.manager.arena;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Marca visualmente os pontos de interesse de uma arena (spawn da arena,
 * spawns de time e geradores) com blocos de destaque, capturando o block data
 * original antes de substituí-lo.
 */
final class ArenaMarkerBlocks {

    private ArenaMarkerBlocks() {
    }

    /**
     * Marca visualmente o spawn da arena, os spawns de time e os geradores.
     *
     * @param arena arena cujos marcadores serão exibidos (não nula)
     */
    static void show(@NotNull final Arena arena) {
        if (arena.getArenaSpawn() != null) {
            final var block = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0);
            if (arena.getSpawnBlockData() == null) {
                arena.setSpawnBlockData(block.getBlockData().getAsString());
            }
            block.setType(Material.EMERALD_BLOCK, false);
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                final var block = team.getSpawn().getBlock().getRelative(0, -1, 0);
                if (team.getSpawnBlockData() == null) {
                    team.setSpawnBlockData(block.getBlockData().getAsString());
                }
                block.setType(teamConcrete(team.getColor()), false);
            }
        }
        for (final ArenaGenerator generator : arena.getGenerators()) {
            if (generator.getLocation() == null) {
                continue;
            }
            final var below = generator.getLocation().getBlock().getRelative(0, -1, 0);
            if (generator.getOriginBlockData() == null) {
                generator.setOriginBlockData(below.getBlockData().getAsString());
            }
            below.setType(generatorMarker(generator.getType()), false);
        }
    }

    private static Material teamConcrete(final String dyeColor) {
        if (dyeColor == null) {
            return Material.WHITE_CONCRETE;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO"          -> Material.RED_CONCRETE;
            case "BLUE", "AZUL"             -> Material.BLUE_CONCRETE;
            case "GREEN", "VERDE"           -> Material.GREEN_CONCRETE;
            case "YELLOW", "AMARELO"        -> Material.YELLOW_CONCRETE;
            case "PURPLE", "ROXO"           -> Material.PURPLE_CONCRETE;
            case "PINK", "ROSA"             -> Material.PINK_CONCRETE;
            case "ORANGE", "LARANJA"        -> Material.ORANGE_CONCRETE;
            case "CYAN", "CIANO"            -> Material.CYAN_CONCRETE;
            case "LIME", "VERDE_LIMA"       -> Material.LIME_CONCRETE;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_CONCRETE;
            case "GRAY", "CINZA"            -> Material.GRAY_CONCRETE;
            case "BLACK", "PRETO"           -> Material.BLACK_CONCRETE;
            default                         -> Material.WHITE_CONCRETE;
        };
    }

    private static Material generatorMarker(final String type) {
        return switch (type.toLowerCase()) {
            case "iron"    -> Material.IRON_ORE;
            case "gold"    -> Material.GOLD_ORE;
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "forge"   -> Material.BLAST_FURNACE;
            default        -> Material.SPONGE;
        };
    }
}
