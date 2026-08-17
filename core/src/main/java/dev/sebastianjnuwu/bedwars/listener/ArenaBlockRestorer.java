package dev.sebastianjnuwu.bedwars.listener;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.arena.ArenaManager;

/**
 * Responsável por restaurar os marcadores de uma arena quando quebrados no
 * modo edição.
 * <p>
 * Tentativa na ordem: spawn da arena, spawn do time, cama e gerador. Cada um
 * restaura o bloco original, limpa a referência na arena e persiste via
 * {@link ArenaManager#save}.
 * </p>
 */
class ArenaBlockRestorer {

    private final ArenaManager arenaManager;
    private final LangManager lang;

    ArenaBlockRestorer(final ArenaManager arenaManager, final LangManager lang) {
        this.arenaManager = arenaManager;
        this.lang = lang;
    }

    /**
     * Tenta restaurar o marcador do bloco quebrado, na ordem arena spawn,
     * team spawn, cama e gerador.
     *
     * @param arena arena em edição (não nula)
     * @param event evento de quebra de bloco (não nulo)
     */
    void tryRestore(final Arena arena, final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (this.tryRestoreArenaSpawn(arena, block, event)) {
            return;
        }
        if (this.tryRestoreTeamSpawn(arena, block, event)) {
            return;
        }
        if (this.tryRestoreBed(arena, block, event)) {
            return;
        }
        this.tryRestoreGenerator(arena, block, event);
    }

    private boolean tryRestoreArenaSpawn(final Arena arena, final Block block, final BlockBreakEvent event) {
        if (arena.getArenaSpawn() == null || arena.getSpawnBlockData() == null) {
            return false;
        }
        final Location spawnBlock = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0).getLocation();
        if (!this.isSameBlock(spawnBlock, block.getLocation())) {
            return false;
        }
        event.setCancelled(true);
        event.setDropItems(false);
        block.setBlockData(Bukkit.createBlockData(arena.getSpawnBlockData()), false);
        arena.setArenaSpawn(null);
        arena.setSpawnBlockData(null);
        this.arenaManager.save(arena);
        block.getWorld().getPlayers().stream()
                .filter(p -> p.getWorld().equals(block.getWorld()))
                .forEach(p -> CompatProvider.chat().sendMessage(p, this.lang.text(NamedTextColor.YELLOW, "edit.arena_spawn_removed")));
        return true;
    }

    private boolean tryRestoreTeamSpawn(final Arena arena, final Block block, final BlockBreakEvent event) {
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() == null || team.getSpawnBlockData() == null) {
                continue;
            }
            final Location markerLoc = team.getSpawn().getBlock().getRelative(0, -1, 0).getLocation();
            if (!this.isSameBlock(markerLoc, block.getLocation())) {
                continue;
            }
            event.setCancelled(true);
            event.setDropItems(false);
            block.setBlockData(Bukkit.createBlockData(team.getSpawnBlockData()), false);
            team.setSpawn(null);
            team.setSpawnBlockData(null);
            this.arenaManager.save(arena);
            block.getWorld().getPlayers().stream()
                    .filter(p -> p.getWorld().equals(block.getWorld()))
                    .forEach(p -> CompatProvider.chat().sendMessage(p, this.lang.text(NamedTextColor.YELLOW, "edit.team_spawn_removed", team.getName())));
            return true;
        }
        return false;
    }

    private boolean tryRestoreBed(final Arena arena, final Block block, final BlockBreakEvent event) {
        if (!(block.getBlockData() instanceof final Bed bedData)) {
            return false;
        }

        final Location footLoc = this.getBedFootLocation(block);
        if (footLoc == null) {
            return false;
        }

        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getBed() == null) {
                continue;
            }
            if (!this.isSameBlock(team.getBed(), footLoc)) {
                continue;
            }

            event.setCancelled(true);
            event.setDropItems(false);
            // Remove both bed blocks
            footLoc.getBlock().setType(Material.AIR, false);
            if (team.getBedFacing() != null) {
                try {
                    final BlockFace face = BlockFace.valueOf(team.getBedFacing().toUpperCase());
                    final Location headLoc = footLoc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                    headLoc.getBlock().setType(Material.AIR, false);
                } catch (final IllegalArgumentException ignored) { }
            }
            team.setBed(null);
            team.setBedFacing(null);
            this.arenaManager.save(arena);
            block.getWorld().getPlayers().stream()
                    .filter(p -> p.getWorld().equals(block.getWorld()))
                    .forEach(p -> CompatProvider.chat().sendMessage(p, this.lang.text(NamedTextColor.YELLOW, "edit.bed_removed", team.getName())));
            return true;
        }
        return false;
    }

    private void tryRestoreGenerator(final Arena arena, final Block block, final BlockBreakEvent event) {
        final List<ArenaGenerator> gens = arena.getGenerators();
        for (int i = 0; i < gens.size(); i++) {
            final ArenaGenerator gen = gens.get(i);
            final Location loc = gen.getLocation();
            if (loc == null) {
                continue;
            }
            if (this.isSameBlock(loc, block.getLocation())) {
                event.setCancelled(true);
                event.setDropItems(false);
                final Block markerBlock = loc.getBlock();
                if (gen.getOriginBlockData() != null) {
                    markerBlock.setBlockData(Bukkit.createBlockData(gen.getOriginBlockData()), false);
                } else {
                    markerBlock.setType(Material.AIR, false);
                }
                arena.getGenerators().remove(i);
                this.arenaManager.save(arena);
                block.getWorld().getPlayers().stream()
                        .filter(p -> p.getWorld().equals(block.getWorld()))
                        .forEach(p -> CompatProvider.chat().sendMessage(p, this.lang.text(NamedTextColor.YELLOW, "edit.generator_removed", gen.getType())));
                return;
            }
        }
    }

    /**
     * Normaliza a localização do bloco de cama para o pé (foot), independente
     * da parte quebrada/clicada.
     *
     * @param bedBlock bloco de cama (não nulo)
     * @return localização do pé da cama ou {@code null} se não for uma cama
     */
    @Nullable Location getBedFootLocation(final Block bedBlock) {
        if (!(bedBlock.getBlockData() instanceof final Bed bedData)) {
            return null;
        }
        final Location clickedLoc = bedBlock.getLocation();
        if (bedData.getPart() == Bed.Part.HEAD) {
            final BlockFace facing = bedData.getFacing();
            return clickedLoc.clone().add(-facing.getModX(), -facing.getModY(), -facing.getModZ());
        }
        return clickedLoc;
    }

    /**
     * Verifica se duas localizações referem-se ao mesmo bloco (mesmo mundo e coordenadas).
     *
     * @param a primeira localização (não nula)
     * @param b segunda localização (não nula)
     * @return {@code true} se ambas as localizações apontam para o mesmo bloco
     */
    boolean isSameBlock(final Location a, final Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
