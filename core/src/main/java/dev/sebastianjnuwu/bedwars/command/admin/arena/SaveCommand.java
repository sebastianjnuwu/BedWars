package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.world.Schematic;

/**
 * Comando para salvar as alterações de uma arena.
 * <p>
 * Gera um novo schematic a partir das coordenadas de pasta e tamanho
 * definidos na arena, salva o arquivo .bwmap no disco, persiste os
 * dados da arena e encerra a sessão de edição. Ao final, teleporta o
 * jogador de volta ao lobby se este estiver configurado.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena save <nome>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class SaveCommand extends BaseCommand implements SubCommand {

    private final SlimeManager slimeManager;

    /**
     * Construtor do comando de salvamento de arena.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public SaveCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.slimeManager = new SlimeManager(JavaPlugin.getPlugin(BedWarsPlugin.class));
    }

    /**
     * Executa o comando de salvamento de arena.
     * <p>
     * Verifica se a arena existe, se o mundo está carregado e se o jogador
     * tem permissão para salvar (caso esteja sendo editada por outro).
     * Constrói o schematic a partir das coordenadas armazenadas, salva o
     * arquivo .bwmap, persiste a arena e encerra a sessão de edição.
     * Se o lobby estiver configurado, o jogador é teleportado de volta.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.usage"));
            return;
        }
        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.not_found", name));
            return;
        }
        if (sender instanceof final Player player) {
            if (this.editorManager.isBeingEdited(name) && !this.editorManager.isEditing(player, name)) {
                final String editor = this.editorManager.getEditorName(name);
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.not_editing", editor));
                return;
            }
        }
        final String worldName = arena.getWorldName();
        if (worldName == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.not_loaded", name));
            return;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.world_not_found"));
            return;
        }
        try {
            this.restoreOriginalBlocks(arena);

            int pasteX = arena.getPasteX();
            int pasteY = arena.getPasteY();
            int pasteZ = arena.getPasteZ();
            int w = arena.getSchematicWidth();
            int h = arena.getSchematicHeight();
            int l = arena.getSchematicLength();

            // Detecta automaticamente a caixa de contorno dos blocos construídos no mundo de edição.
            final Bounds bounds = this.detectBuiltBounds(world);
            if (bounds != null) {
                pasteX = bounds.minX;
                pasteY = bounds.minY;
                pasteZ = bounds.minZ;
                w = bounds.maxX - bounds.minX + 1;
                h = bounds.maxY - bounds.minY + 1;
                l = bounds.maxZ - bounds.minZ + 1;
                arena.setPaste(pasteX, pasteY, pasteZ);
                arena.setSchematicSize(w, h, l);
                org.bukkit.Bukkit.getLogger().info(this.lang.raw("log.save_command.auto_dimensions",
                        name, String.valueOf(w), String.valueOf(h), String.valueOf(l),
                        String.valueOf(pasteX), String.valueOf(pasteY), String.valueOf(pasteZ)));
            }

            if (w <= 0 || h <= 0 || l <= 0) {
                org.bukkit.Bukkit.getLogger().severe(this.lang.raw("log.save_command.invalid_dimensions", name, String.valueOf(w), String.valueOf(h), String.valueOf(l)));
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.invalid_dimensions"));
                return;
            }

            final Location pos1 = new Location(world, pasteX, pasteY, pasteZ);
            final Location pos2 = new Location(world, pasteX + w - 1, pasteY + h - 1, pasteZ + l - 1);
            final Schematic schematic = new Schematic(name, pos1, pos2);

            final File mapFile = new File(this.mapsFolder, name + ".schem");
            this.mapsFolder.mkdirs();
            org.bukkit.Bukkit.getLogger().info(this.lang.raw("log.save_command.saving_schematic",
                    name, mapFile.getAbsolutePath(),
                    String.valueOf(pasteX), String.valueOf(pasteY), String.valueOf(pasteZ),
                    String.valueOf(pasteX + w - 1), String.valueOf(pasteY + h - 1),
                    String.valueOf(pasteZ + l - 1)));

            try {
                schematic.save(mapFile, world);
                org.bukkit.Bukkit.getLogger().info(this.lang.raw("log.save_command.schematic_saved", mapFile.getAbsolutePath(), String.valueOf(w), String.valueOf(h), String.valueOf(l)));
            } catch (final IOException e) {
                org.bukkit.Bukkit.getLogger().log(Level.SEVERE, this.lang.raw("log.save_command.schematic_save_error", name, e.getMessage()), e);
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.map_save_fail", e.getMessage()));
                return;
            }

            if (this.slimeManager.isAvailable()) {
                this.slimeManager.saveTemplate(name, world)
                        .thenRun(() -> org.bukkit.Bukkit.getLogger().info(this.lang.raw("log.save_command.slime_template_saved", name)))
                        .exceptionally(ex -> {
                            org.bukkit.Bukkit.getLogger().log(Level.WARNING, this.lang.raw("log.save_command.slime_template_error", name, ex.getMessage()), ex);
                            return null;
                        });
            }

            this.arenaManager.save(arena);
            this.arenaManager.flush(arena.getName());
            this.arenaManager.markWorldClean(worldName);
            this.gameManager.getShopNpcManager().removeEditorNpcs(arena.getName());
            this.editorManager.endSession(name);

            // ── YML validation report ────────────────────────────────────
            final java.util.List<String> missing = this.gameManager.validateArena(arena);
            if (missing.isEmpty()) {
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "save.success", name));
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "save.validation_ok"));
            } else {
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.YELLOW, "save.success_with_warnings", name));
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.YELLOW, "save.validation_warnings"));
                for (final String warn : missing) {
                    CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "game.missing_entry", warn));
                }
            }

            if (sender instanceof final Player player) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                final Location lobby = this.configManager.getLobby();
                if (lobby != null) {
                    player.teleport(lobby);
                } else {
                    CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.YELLOW, "no_lobby"));
                }
            }
        } catch (final Exception e) {
            org.bukkit.Bukkit.getLogger().log(Level.SEVERE, this.lang.raw("log.save_command.save_unexpected_error", name, e.getMessage()), e);
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "save.error", e.getMessage()));
        }
    }

    private void restoreOriginalBlocks(final Arena arena) {
        if (arena.getArenaSpawn() != null && arena.getSpawnBlockData() != null) {
            final var b = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0);
            b.setBlockData(Bukkit.createBlockData(arena.getSpawnBlockData()), false);
        }
        for (final var team : arena.getTeams()) {
            if (team.getSpawn() != null && team.getSpawnBlockData() != null) {
                final var b = team.getSpawn().getBlock().getRelative(0, -1, 0);
                b.setBlockData(Bukkit.createBlockData(team.getSpawnBlockData()), false);
            }
        }
        for (final var gen : arena.getGenerators()) {
            if (gen.getOriginBlockData() != null && gen.getLocation() != null) {
                final var b = gen.getLocation().getBlock().getRelative(0, -1, 0);
                b.setBlockData(Bukkit.createBlockData(gen.getOriginBlockData()), false);
            }
        }
    }

    /**
     * Detecta a caixa de contorno dos blocos construídos no mundo de edição.
     * <p>
     * Como o mundo é void, os únicos blocos não-ar são o mapa e as alterações
     * feitas pelo administrador. Isso permite salvar o schematic sem depender
     * de seleção manual (//pos1 e //pos2) a cada edição.
     * </p>
     *
     * @param world mundo de edição
     * @return os limites da área construída, ou {@code null} se não houver blocos
     */
    private @org.jetbrains.annotations.Nullable Bounds detectBuiltBounds(final World world) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        boolean found = false;
        for (final Chunk chunk : world.getLoadedChunks()) {
            final int baseX = chunk.getX() << 4;
            final int baseZ = chunk.getZ() << 4;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        if (chunk.getBlock(x, y, z).getType().isAir()) {
                            continue;
                        }
                        found = true;
                        final int wx = baseX + x;
                        final int wz = baseZ + z;
                        if (wx < minX) {
                            minX = wx;
                        }
                        if (wx > maxX) {
                            maxX = wx;
                        }
                        if (wz < minZ) {
                            minZ = wz;
                        }
                        if (wz > maxZ) {
                            maxZ = wz;
                        }
                        if (y < minY) {
                            minY = y;
                        }
                        if (y > maxY) {
                            maxY = y;
                        }
                    }
                }
            }
        }
        if (!found) {
            return null;
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }
}
