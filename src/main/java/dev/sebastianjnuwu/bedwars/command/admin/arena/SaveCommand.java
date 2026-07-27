package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import net.kyori.adventure.text.format.NamedTextColor;

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
        this.slimeManager = new SlimeManager(org.bukkit.Bukkit.getPluginManager().getPlugin("BedWars"));
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
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.usage"));
            return;
        }
        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.not_found", name));
            return;
        }
        if (sender instanceof final Player player) {
            if (this.editorManager.isBeingEdited(name) && !this.editorManager.isEditing(player, name)) {
                final String editor = this.editorManager.getEditorName(name);
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.not_editing", editor));
                return;
            }
        }
        final String worldName = arena.getWorldName();
        if (worldName == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.not_loaded", name));
            return;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.world_not_found"));
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

            // Se as dimensões não foram definidas, tenta ler da seleção do FAWE
            if ((w <= 0 || h <= 0 || l <= 0) && sender instanceof final Player player) {
                try {
                    final com.sk89q.worldedit.entity.Player wePlayer = BukkitAdapter.adapt(player);
                    final LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
                    final Region selection = session.getSelection(wePlayer.getWorld());
                    final BlockVector3 min = selection.getMinimumPoint();
                    final BlockVector3 max = selection.getMaximumPoint();

                    pasteX = min.getBlockX();
                    pasteY = min.getBlockY();
                    pasteZ = min.getBlockZ();
                    w = max.getBlockX() - min.getBlockX() + 1;
                    h = max.getBlockY() - min.getBlockY() + 1;
                    l = max.getBlockZ() - min.getBlockZ() + 1;

                    arena.setPaste(pasteX, pasteY, pasteZ);
                    arena.setSchematicSize(w, h, l);

                    org.bukkit.Bukkit.getLogger().info(
                            "Dimensões lidas da seleção FAWE para arena '" + name + "': "
                                    + w + "x" + h + "x" + l + " (paste em "
                                    + pasteX + "," + pasteY + "," + pasteZ + ")");
                } catch (final IncompleteRegionException | NullPointerException e) {
                    sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.error",
                            "Seleção do FAWE incompleta. Use //pos1 e //pos2 primeiro."));
                    return;
                }
            }

            if (w <= 0 || h <= 0 || l <= 0) {
                org.bukkit.Bukkit.getLogger().severe(
                        "Dimensões inválidas do schematic para arena '" + name + "': " + w + "x" + h + "x" + l);
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.error",
                        "Dimensões do schematic inválidas. Use //pos1 e //pos2 para selecionar a área."));
                return;
            }

            final Location pos1 = new Location(world, pasteX, pasteY, pasteZ);
            final Location pos2 = new Location(world, pasteX + w - 1, pasteY + h - 1, pasteZ + l - 1);
            final Schematic schematic = new Schematic(name, pos1, pos2);

            final File mapFile = new File(this.mapsFolder, name + ".schem");
            this.mapsFolder.mkdirs();
            org.bukkit.Bukkit.getLogger().info("Salvando schematic da arena '" + name + "' em "
                    + mapFile.getAbsolutePath()
                    + " (região: " + pasteX + "," + pasteY + "," + pasteZ + " a "
                    + (pasteX + w - 1) + "," + (pasteY + h - 1) + "," + (pasteZ + l - 1) + ")");

            try {
                schematic.save(mapFile, world);
                org.bukkit.Bukkit.getLogger().info("Schematic salvo com sucesso: " + mapFile.getAbsolutePath()
                        + " (" + w + "x" + h + "x" + l + " blocos)");
            } catch (final IOException e) {
                org.bukkit.Bukkit.getLogger().log(Level.SEVERE,
                        "Falha ao salvar schematic da arena '" + name + "': " + e.getMessage(), e);
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.error",
                        "Falha ao salvar o arquivo do mapa: " + e.getMessage()));
                return;
            }

            if (this.slimeManager.isAvailable()) {
                this.slimeManager.saveTemplate(name, world)
                        .thenRun(() -> org.bukkit.Bukkit.getLogger().info(
                                "Template Slime salvo: " + name))
                        .exceptionally(ex -> {
                            org.bukkit.Bukkit.getLogger().log(Level.WARNING,
                                    "Falha ao salvar template Slime para arena '" + name + "': "
                                            + ex.getMessage(), ex);
                            return null;
                        });
            }

            this.arenaManager.save(arena);
            this.arenaManager.flush(arena.getName());
            this.editorManager.endSession(name);

            // ── YML validation report ────────────────────────────────────
            final java.util.List<String> missing = this.gameManager.validateArena(arena);
            if (missing.isEmpty()) {
                sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "save.success", name));
                sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "save.validation_ok"));
            } else {
                sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "save.success_with_warnings", name));
                sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "save.validation_warnings"));
                for (final String warn : missing) {
                    sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.missing_entry", warn));
                }
            }

            if (sender instanceof final Player player) {
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                final Location lobby = this.configManager.getLobby();
                if (lobby != null) {
                    player.teleport(lobby);
                } else {
                    player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "no_lobby"));
                }
            }
        } catch (final Exception e) {
            org.bukkit.Bukkit.getLogger().log(Level.SEVERE,
                    "Erro inesperado ao salvar arena '" + name + "': " + e.getMessage(), e);
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "save.error", e.getMessage()));
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
            if (gen.getOriginBlockData() != null) {
                final var b = gen.getLocation().getBlock().getRelative(0, -1, 0);
                b.setBlockData(Bukkit.createBlockData(gen.getOriginBlockData()), false);
            }
        }
    }
}
