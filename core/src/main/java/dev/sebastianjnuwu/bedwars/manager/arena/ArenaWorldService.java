package dev.sebastianjnuwu.bedwars.manager.arena;

import java.io.File;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Responsável pelos mundos das arenas: construção/reconstrução a partir do
 * schematic, instâncias de partida, carregamento, restauração de camas,
 * marcadores de spawn/geradores e resolução do arquivo de mapa.
 * <p>
 * A restauração de camas, os marcadores visuais e a resolução de mapa ficam
 * em {@link ArenaBedRestorer}, {@link ArenaMarkerBlocks} e {@link MapFileResolver}.
 * </p>
 */
public final class ArenaWorldService {

    private final ArenaManager manager;
    private final MapFileResolver mapResolver;

    /**
     * Cria o serviço de mundos de arenas.
     *
     * @param manager arena manager que será alcançado por este serviço (não nulo)
     */
    public ArenaWorldService(final ArenaManager manager) {
        this.manager = manager;
        this.mapResolver = new MapFileResolver(manager.mapsFolder);
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
        final World world = this.manager.worldProvider.buildWorld(name, worldName, this.mapResolver.forArena(arena), arena,
                "log.arena_manager.reset_error");
        if (world == null) {
            this.manager.markWorldDirty(worldName);
            return false;
        }
        this.manager.markWorldClean(worldName);
        arena.setWorldName(worldName);
        ArenaWorldReferenceUpdater.update(arena, world);
        ArenaBedRestorer.restore(world, arena);
        this.manager.flush(arena.getName());
        ArenaMarkerBlocks.show(this.manager.get(name));
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
        final World world = this.manager.worldProvider.buildWorld(resolved, worldName, this.mapResolver.forArena(arena), arena,
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
        ArenaBedRestorer.restore(world, instance);
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
        this.manager.worldProvider.buildWorldAsync(resolved, worldName, this.mapResolver.forArena(arena), arena,
                "log.arena_manager.load_error", world -> {
                    if (world == null) {
                        this.manager.markWorldDirty(worldName);
                        callback.accept(null);
                        return;
                    }
                    try {
                        final Arena instance = this.manager.persistence().load(resolved, file, world);
                        instance.setWorldName(worldName);
                        ArenaBedRestorer.restore(world, instance);
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
        return this.mapResolver.byName(name);
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
        return this.mapResolver.forArena(arena);
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
                ArenaWorldReferenceUpdater.update(refreshed, world);
                ArenaBedRestorer.restore(world, refreshed);
            }
            this.manager.flush(arena.getName());
            return world;
        }
        world = this.manager.worldProvider.buildWorld(arena.getName(), worldName, this.mapResolver.forArena(arena), arena,
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
            ArenaWorldReferenceUpdater.update(refreshed, world);
            ArenaBedRestorer.restore(world, refreshed);
        }
        this.manager.flush(arena.getName());
        return world;
    }

    /**
     * Restaura as camas das equipes após a reconstrução do mundo.
     * <p>
     * Como a cama pode ficar fora dos limites do schematic, ela é recolocada
     * programaticamente a partir da configuração da arena (local + direção).
     * </p>
     */
    public void restoreBeds(final World world, final Arena arena) {
        ArenaBedRestorer.restore(world, arena);
    }

    /**
     * Marca visualmente o spawn da arena, os spawns de time e os geradores com
     * blocos de destaque, capturando o block data original antes de substituí-lo.
     *
     * @param arena arena cujos marcadores serão exibidos (não nula)
     */
    public void showMarkerBlocks(final Arena arena) {
        ArenaMarkerBlocks.show(arena);
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
