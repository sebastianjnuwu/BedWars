package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import dev.sebastianjnuwu.bedwars.util.LocationUtil;
import dev.sebastianjnuwu.bedwars.world.Schematic;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Comando para carregar uma arena no mundo do servidor.
 * <p>
 * Cria um novo mundo utilizando um {@link VoidGenerator}, carrega o
 * schematic da arena a partir do arquivo .bwmap e cola o schematic no
 * spawn do mundo. Inicia automaticamente uma sessão de edição para o
 * jogador.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena load <nome>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class LoadCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de carregamento de arena.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public LoadCommand(
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
     * Executa o comando de carregamento de arena.
     * <p>
     * Valida os argumentos, verifica se a arena existe e se o arquivo .bwmap
     * está presente. Cria um novo mundo com gerador vazio, cola o schematic
     * no spawn e inicia a sessão de edição para o jogador. Caso o mundo já
     * exista, exibe uma mensagem de erro apropriada.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.usage"));
            return;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.only_player"));
            return;
        }
        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.not_found", name));
            return;
        }
        final String worldName = "bw_" + name;
        if (Bukkit.getWorld(worldName) != null) {
            if (this.editorManager.isBeingEdited(name)) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.already_being_edited",
                        this.editorManager.getEditorName(name)));
                return;
            }
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.already_loaded", name));
            return;
        }

        final File file = this.arenaManager.getMapFile(name);
        if (file == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.file_not_found"));
            return;
        }

        try {
            final WorldCreator wc = new WorldCreator(worldName);
            wc.generator(new dev.sebastianjnuwu.bedwars.world.VoidGenerator());
            final World world = wc.createWorld();
            if (world == null) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.error",
                        "Nao foi possivel criar o mundo."));
                return;
            }
            final Schematic schematic = Schematic.load(arena.getName(), file);
            final Location pasteLoc = arena.getPasteX() != 0 || arena.getPasteY() != 0 || arena.getPasteZ() != 0
                    ? new Location(world, arena.getPasteX(), arena.getPasteY(), arena.getPasteZ())
                    : world.getSpawnLocation();
            schematic.paste(world, pasteLoc, file);
            this.arenaManager.applyWorldSettings(world, arena);
            this.arenaManager.reload(arena.getName());
            final Arena refreshed = this.arenaManager.get(arena.getName());
            if (refreshed != null) {
                refreshed.setWorldName(worldName);
                this.arenaManager.showMarkerBlocks(refreshed);
                this.arenaManager.save(refreshed);
                this.arenaManager.flush(refreshed.getName());
            }
            this.editorManager.startSession(player, name);
            LocationUtil.safeTeleport(player, pasteLoc);
            player.setGameMode(org.bukkit.GameMode.CREATIVE);
            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "load.success"));
        } catch (final Exception e) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "load.error", e.getMessage()));
        }
    }
}
