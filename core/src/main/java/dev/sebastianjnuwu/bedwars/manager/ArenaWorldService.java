package dev.sebastianjnuwu.bedwars.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;

/**
 * Responsável pelos mundos das arenas: construção/reconstrução a partir do
 * schematic, instâncias de partida, carregamento, restauração de camas,
 * marcadores de spawn/geradores e resolução do arquivo de mapa.
 */
public final class ArenaWorldService {

    private final ArenaManager manager;

    /**
     * Cria o serviço de mundos de arenas.
     *
     * @param manager arena manager que será alcançado por este serviço (não nulo)
     */
    public ArenaWorldService(final ArenaManager manager) {
        this.manager = manager;
    }

    /**
     * Garante que o mundo da arena está carregado e recarrega os dados do yml.
     *
     * @param arena arena cujo mundo deve estar pronto (não nula)
     * @return {@code true} se a arena está pronta
     */
    public boolean ensureArenaReady(final Arena arena) {
        if (arena == null) {
            return false;
        }
        final World world = this.ensureWorldLoaded(arena);
        if (world == null) {
            return false;
        }
        this.manager.reload(arena.getName());
        final Arena refreshed = this.manager.get(arena.getName());
        return refreshed != null;
    }

    /**
     * Reconstrói o mundo da arena a partir do schematic e restaura os blocos
     * especiais (camas e marcadores).
     *
     * @param name nome da arena (não nulo)
     * @return {@code true} se o reset foi concluído
     */
    public boolean resetArenaMap(final String name) {
        final Arena arena = this.manager.get(name);
        if (arena == null) {
            return false;
        }
        final String worldName = "bw_" + name;
        final World world = this.manager.worldProvider.buildWorld(name, worldName, this.getMapFile(arena), arena,
                "log.arena_manager.reset_error");
        if (world == null) {
            this.manager.markWorldDirty(worldName);
            return false;
        }
        this.manager.markWorldClean(worldName);
        arena.setWorldName(worldName);
        this.updateWorldReferences(arena, world);
        this.restoreBeds(world, arena);
        this.manager.flush(arena.getName());
        this.showMarkerBlocks(this.manager.get(name));
        return true;
    }

    /**
     * Cria uma nova instância de partida para uma arena, clonando a configuração
     * e construindo um mundo de partida dedicado ({@code bw_<arena>_<id>}).
     * <p>
     * Isso permite que uma única arena hospede várias partidas simultâneas do
     * mesmo mapa, cada uma isolada em seu próprio mundo, recriado a partir do
     * schematic a cada instância.
     * </p>
     *
     * @param arenaName nome da arena (não nulo)
     * @return a arena clonada com o mundo de partida pronto, ou {@code null} se falhar
     */
    public @Nullable Arena createInstance(final String arenaName) {
        final Arena arena = this.manager.get(arenaName);
        if (arena == null) {
            return null;
        }
        final String resolved = arena.getName();
        final File file = this.manager.persistence().findArenaFile(resolved);
        if (file == null) {
            return null;
        }
        final String worldName = this.nextInstanceWorldName(resolved);

        // Mundo construído com as configurações não-posicionais da arena em memória
        // (paste, mapa, difficulty... — independem de locations resolvidas).
        final World world = this.manager.worldProvider.buildWorld(resolved, worldName, this.getMapFile(arena), arena,
                "log.arena_manager.load_error");
        if (world == null) {
            this.manager.markWorldDirty(worldName);
            return null;
        }
        this.manager.markWorldClean(worldName);
        // Instância reconstruída 100% do disco, com todas as locations rebasadas
        // para o mundo de partida recém-criado.
        final Arena instance = this.manager.persistence().load(resolved, file, world);
        instance.setWorldName(worldName);
        this.restoreBeds(world, instance);
        return instance;
    }

    /**
     * Cria uma instância de partida de forma assíncrona, delegando a construção
     * do mundo ao backend ativo (o paste do schematic acontece fora da main
     * thread) e invocando o callback na main thread quando o mundo estiver pronto.
     * <p>
     * Quando finalizado, o callback recebe a arena clonada com todas as
     * locations rebasadas, ou {@code null} se a construção falhar.
     * </p>
     *
     * @param arenaName nome da arena (não nulo)
     * @param callback  consumidor chamado na main thread com a instância pronta (não nulo)
     */
    public void createInstanceAsync(final String arenaName, final Consumer<Arena> callback) {
        final Arena arena = this.manager.get(arenaName);
        if (arena == null) {
            callback.accept(null);
            return;
        }
        final String resolved = arena.getName();
        final File file = this.manager.persistence().findArenaFile(resolved);
        if (file == null) {
            callback.accept(null);
            return;
        }
        final String worldName = this.nextInstanceWorldName(resolved);
        this.manager.worldProvider.buildWorldAsync(resolved, worldName, this.getMapFile(arena), arena,
                "log.arena_manager.load_error", world -> {
                    if (world == null) {
                        this.manager.markWorldDirty(worldName);
                        callback.accept(null);
                        return;
                    }
                    try {
                        final Arena instance = this.manager.persistence().load(resolved, file, world);
                        instance.setWorldName(worldName);
                        this.restoreBeds(world, instance);
                        this.manager.markWorldClean(worldName);
                        callback.accept(instance);
                    } catch (final Exception e) {
                        this.manager.plugin.getLogger().severe(this.manager.lang.raw("log.arena_manager.load_error", resolved, e.getMessage()));
                        this.manager.markWorldDirty(worldName);
                        callback.accept(null);
                    }
                });
    }

    /**
     * Remove o mundo de uma instância de partida (unload + exclusão do disco).
     *
     * @param worldName nome do mundo de partida
     */
    public void deleteInstanceWorld(final String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        this.manager.worldProvider.deleteWorld(worldName);
        this.manager.markWorldDirty(worldName);
    }

    private String nextInstanceWorldName(final String arenaName) {
        int id = this.manager.instanceCounters.getOrDefault(arenaName, 0);
        String worldName;
        do {
            worldName = "bw_" + arenaName + "_" + id;
            id++;
        } while (new File(Bukkit.getWorldContainer(), worldName).exists());
        this.manager.instanceCounters.put(arenaName, id);
        return worldName;
    }

    /**
     * Aplica as configurações de mundo (difficulty, tempo, clima, gamerules) à arena.
     *
     * @param world mundo a configurar (não nulo)
     * @param arena arena com as configurações (não nula)
     */
    public void applyWorldSettings(final World world, final Arena arena) {
        this.manager.worldProvider.applyWorldSettings(world, arena);
    }

    /**
     * Resolve o arquivo de mapa de uma arena pelo nome.
     *
     * @param name nome da arena
     * @return arquivo do schematic ou {@code null}
     */
    public @Nullable File getMapFile(final String name) {
        if (name == null) {
            return null;
        }
        // Priorizar formato interno .bwmap
        File file = new File(this.manager.mapsFolder, name + ".bwmap");
        if (!file.exists()) {
            file = new File(this.manager.mapsFolder, name + ".schem");
        }
        if (!file.exists()) {
            file = new File(this.manager.mapsFolder, name + ".schematic");
        }
        if (!file.exists()) {
            file = new File(this.manager.mapsFolder, name + ".nbt");
        }
        if (!file.exists()) {
            file = new File(this.manager.mapsFolder, name);
        }
        if (file.exists()) {
            return file;
        }
        final File[] files = this.manager.mapsFolder.listFiles();
        if (files != null) {
            for (final File candidate : files) {
                if (candidate.isFile() && stripMapExtension(candidate.getName()).equalsIgnoreCase(name)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Resolve o arquivo de mapa usado por uma arena, considerando o mapa
     * compartilhado ({@link Arena#getMapName()}) quando configurado. Isso
     * permite que várias arenas rodem partidas simultâneas do mesmo mapa.
     *
     * @param arena arena cujo mapa deve ser resolvido (não nula)
     * @return arquivo do schematic, ou {@code null} se não encontrado
     */
    public @Nullable File getMapFile(final Arena arena) {
        if (arena == null) {
            return null;
        }
        final String mapName = arena.getMapName();
        final String resolved = mapName == null || mapName.isBlank() ? arena.getName() : mapName;
        return this.getMapFile(resolved);
    }

    private static String stripMapExtension(final String fileName) {
        final int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    /**
     * Garante que o mundo da arena está carregado e com as referências atualizadas.
     * <p>
     * Quando o mundo já está carregado e sem mudanças pendentes (ex.: após um
     * unload/load do plugin), a arena é recarregada do yml e suas localizações
     * (spawns, camas, geradores e NPCs) são re-sincronizadas em memória. Isso evita
     * que o {@code validateArena} acuse configurações faltando após o reload.
     * Caso contrário, o mundo é reconstruído a partir do schematic.
     * </p>
     *
     * @param arena a arena cujo mundo deve estar pronto (não nula)
     * @return o mundo carregado/pronto, ou {@code null} se não foi possível
     */
    public @Nullable World ensureWorldLoaded(final Arena arena) {
        String worldName = arena.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            worldName = "bw_" + arena.getName();
        }
        World world = Bukkit.getWorld(worldName);
        // Mundo já carregado e sem mudanças pendentes (ex.: após unload/load do plugin).
        // Recarrega o yml e atualiza as referências em memória para que spawns, camas,
        // geradores e NPCs não fiquem nulos e o validateArena não acuse configuração faltando.
        if (world != null && this.manager.cleanWorlds.contains(worldName)) {
            this.applyWorldSettings(world, arena);
            this.manager.reload(arena.getName());
            final Arena refreshed = this.manager.get(arena.getName());
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
                this.updateWorldReferences(refreshed, world);
                this.restoreBeds(world, refreshed);
            }
            this.manager.flush(arena.getName());
            return world;
        }
        world = this.manager.worldProvider.buildWorld(arena.getName(), worldName, this.getMapFile(arena), arena,
                "log.arena_manager.load_error");
        if (world == null) {
            this.manager.markWorldDirty(worldName);
            return null;
        }
        this.manager.markWorldClean(worldName);
        this.manager.reload(arena.getName());
        final Arena refreshed = this.manager.get(arena.getName());
        if (refreshed != null) {
            refreshed.setWorldName(worldName);
            this.updateWorldReferences(refreshed, world);
            this.restoreBeds(world, refreshed);
        }
        this.manager.flush(arena.getName());
        return world;
    }

    private void updateWorldReferences(final Arena arena, final World newWorld) {
        if (arena.getArenaSpawn() != null) {
            final Location old = arena.getArenaSpawn();
            arena.setArenaSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        if (arena.getLobby() != null) {
            final Location old = arena.getLobby();
            arena.setLobby(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                final Location old = team.getSpawn();
                team.setSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
            if (team.getBed() != null) {
                final Location old = team.getBed();
                team.setBed(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        for (final ArenaGenerator gen : arena.getGenerators()) {
            if (gen.getLocation() != null) {
                final Location old = gen.getLocation();
                gen.setLocation(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        if (arena.getShopNpcs() != null) {
            final List<ShopNpc> updated = new ArrayList<>();
            for (final ShopNpc npc : arena.getShopNpcs()) {
                final Location old = npc.location();
                final Location newLoc = new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch());
                updated.add(new ShopNpc(newLoc, npc.skin(), npc.displayName()));
            }
            arena.setShopNpcs(updated);
        }
    }

    /**
     * Restaura as camas das equipes após a reconstrução do mundo.
     * <p>
     * Como a cama pode ficar fora dos limites do schematic, ela é recolocada
     * programaticamente a partir da configuração da arena (local + direção).
     * </p>
     */
    public void restoreBeds(final World world, final Arena arena) {
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
            final Material material = this.getBedMaterial(team.getColor());
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

    private Material getBedMaterial(final String dyeColor) {
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

    /**
     * Marca visualmente o spawn da arena, os spawns de time e os geradores com
     * blocos de destaque, capturando o block data original antes de substituí-lo.
     *
     * @param arena arena cujos marcadores serão exibidos (não nula)
     */
    public void showMarkerBlocks(final Arena arena) {
        if (arena == null) {
            return;
        }
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
                block.setType(getTeamConcreteMaterial(team.getColor()), false);
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
            final Material marker = this.getGeneratorMarker(generator.getType());
            below.setType(marker, false);
        }
    }

    private Material getTeamConcreteMaterial(final String dyeColor) {
        if (dyeColor == null) {
            return Material.WHITE_CONCRETE;
        }
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO"         -> Material.RED_CONCRETE;
            case "BLUE", "AZUL"            -> Material.BLUE_CONCRETE;
            case "GREEN", "VERDE"          -> Material.GREEN_CONCRETE;
            case "YELLOW", "AMARELO"       -> Material.YELLOW_CONCRETE;
            case "PURPLE", "ROXO"          -> Material.PURPLE_CONCRETE;
            case "PINK", "ROSA"            -> Material.PINK_CONCRETE;
            case "ORANGE", "LARANJA"       -> Material.ORANGE_CONCRETE;
            case "CYAN", "CIANO"           -> Material.CYAN_CONCRETE;
            case "LIME", "VERDE_LIMA"      -> Material.LIME_CONCRETE;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_CONCRETE;
            case "GRAY", "CINZA"           -> Material.GRAY_CONCRETE;
            case "BLACK", "PRETO"          -> Material.BLACK_CONCRETE;
            default                        -> Material.WHITE_CONCRETE;
        };
    }

    private Material getGeneratorMarker(final String type) {
        return switch (type.toLowerCase()) {
            case "iron"    -> Material.IRON_ORE;
            case "gold"    -> Material.GOLD_ORE;
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "forge"   -> Material.BLAST_FURNACE;
            default        -> Material.SPONGE;
        };
    }

    /**
     * Exclui recursivamente um diretório (usado para remover templates de mapa).
     *
     * @param path diretório a excluir (não nulo)
     */
    public void deleteDirectory(final File path) {
        if (!path.exists()) {
            return;
        }
        final File[] files = path.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    this.deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        path.delete();
    }
}