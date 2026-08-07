package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import dev.sebastianjnuwu.bedwars.world.VoidGenerator;

/**
 * Comando para entrar no modo de edição de uma arena.
 * <p>
 * Teleporta o jogador para o mundo da arena e inicia uma sessão de edição
 * através do {@link EditorManager}. Impede que múltiplos jogadores editem
 * a mesma arena simultaneamente. Se o mundo não estiver carregado, ele é
 * carregado automaticamente.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena edit <nome>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class EditCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de edição de arena.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public EditCommand(
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
     * Executa o comando de edição de arena.
     * <p>
     * Verifica se o nome da arena foi informado e se a arena existe.
     * Carrega automaticamente o mundo se ainda não estiver carregado,
     * coloca o jogador em modo Criativo e inicia a sessão de edição.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.usage"));
            return;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.only_player"));
            return;
        }
        final String name = args[1];
        Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.not_found", name));
            return;
        }

        if (this.editorManager.isBeingEdited(name) && !this.editorManager.isEditing(player, name)) {
            final String editorName = this.editorManager.getEditorName(name);
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.already_editing", name, editorName));
            return;
        }

        if (this.gameManager.getGame(name) != null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.in_progress", name));
            return;
        }

        String worldName = arena.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            worldName = "bw_" + name;
        }

        World world = Bukkit.getWorld(worldName);
        final boolean reusingSession = world != null && this.editorManager.isBeingEdited(name);
        if (world == null || (!this.arenaManager.isWorldClean(worldName) && !reusingSession)) {
            if (world != null && !this.arenaManager.getWorldManager().deleteWorld(worldName)) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.world_not_found"));
                return;
            }
            final WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new VoidGenerator());
            world = wc.createWorld();
            if (world == null) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.world_not_found"));
                return;
            }
            final File file = this.arenaManager.getMapFile(arena);
            if (file != null) {
                try {
                    final Schematic schematic = Schematic.load(name, file);
                    final Location pasteLoc = arena.getPasteX() != 0 || arena.getPasteY() != 0 || arena.getPasteZ() != 0
                            ? new Location(world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ())
                            : world.getSpawnLocation();
                    schematic.paste(world, pasteLoc, file);
                } catch (final Exception ignored) {
                }
            }
            this.arenaManager.applyWorldSettings(world, arena);
            this.arenaManager.reload(arena.getName());
            final Arena refreshed = this.arenaManager.get(arena.getName());
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
                this.arenaManager.save(refreshed);
                this.arenaManager.flush(refreshed.getName());
                arena = refreshed;
            }
            this.arenaManager.markWorldClean(worldName);
        }

        this.arenaManager.showMarkerBlocks(arena);
        this.arenaManager.markWorldDirty(worldName);
        this.editorManager.startSession(player, name);
        this.editorManager.startParticleTask(player, name, this.arenaManager);

        // Spawn shop NPCs for editor view
        this.gameManager.getShopNpcManager().spawnEditorNpcs(
                arena.getName(),
                arena.getShopNpcs()
        );

        // Teleport to the arena spawn if already set, otherwise fall back to world spawn
        final Location destination = arena.getArenaSpawn() != null
                ? arena.getArenaSpawn().clone()
                : world.getSpawnLocation();
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        LocationUtil.safeTeleport(player, destination);
        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "edit.teleported", name));
    }

}
