package dev.sebastianjnuwu.bedwars.command.admin.arena;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Comando para entrar no modo de edição de uma arena.
 * <p>
 * Teleporta o jogador para o mundo da arena e inicia uma sessão de edição
 * através do {@link EditorManager}. Impede que múltiplos jogadores editem
 * a mesma arena simultaneamente.
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
     * Verifica se o nome da arena foi informado, se a arena existe e se o
     * mundo dela está carregado. Caso a arena já esteja sendo editada por
     * outro jogador, a operação é bloqueada. Caso contrário, inicia a
     * sessão de edição e teleporta o jogador para o mundo da arena.
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
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.not_found", name));
            return;
        }
        final String worldName = arena.getWorldName();
        if (worldName == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.not_loaded", name));
            return;
        }
        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.world_not_found"));
            return;
        }
        if (this.editorManager.isBeingEdited(name)) {
            final String editorName = this.editorManager.getEditorName(name);
            if (!this.editorManager.isEditing(player, name)) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "edit.already_editing", name, editorName));
                return;
            }
        } else {
            this.editorManager.startSession(player, name);
        }
        player.teleport(world.getSpawnLocation());
        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "edit.teleported", name));
    }
}
