package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
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
            final Location pos1 = new Location(
                    world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ());
            final Location pos2 = new Location(
                    world,
                    arena.getPasteX() + arena.getSchematicWidth() - 1,
                    arena.getPasteY() + arena.getSchematicHeight() - 1,
                    arena.getPasteZ() + arena.getSchematicLength() - 1);
            final Schematic schematic = new Schematic(name, pos1, pos2);
            schematic.save(new File(this.mapsFolder, name + ".bwmap"));
            this.arenaManager.save(arena);
            this.editorManager.endSession(name);
            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "save.success", name));
            if (sender instanceof final Player player) {
                final Location lobby = this.configManager.getLobby();
                if (lobby != null) {
                    player.teleport(lobby);
                } else {
                    player.sendMessage(this.lang.text(NamedTextColor.YELLOW, "no_lobby"));
                }
            }
        } catch (final Exception e) {
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
