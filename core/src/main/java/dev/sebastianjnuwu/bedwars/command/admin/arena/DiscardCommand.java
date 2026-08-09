package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando para sair do modo de edição de uma arena sem salvar as alterações.
 * <p>
 * Encerra a sessão de edição, remove os NPCs da loja e descarta as mudanças
 * de blocos recriando o mundo a partir do último schematic salvo. O jogador
 * é teleportado de volta ao lobby e retorna ao modo Sobrevivência.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin discard <nome>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class DiscardCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de descarte de edição.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public DiscardCommand(
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
     * Executa o comando de descarte de edição.
     * <p>
     * Valida os argumentos, verifica se a arena existe e se o remetente está
     * editando-a. Encerra a sessão, remove os NPCs da loja e restaura o mundo
     * a partir do schematic salvo, descartando as alterações de blocos.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "discard.usage"));
            return;
        }
        if (!(sender instanceof final Player player)) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "discard.only_player"));
            return;
        }
        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "discard.not_found", name));
            return;
        }
        if (!this.editorManager.isEditing(player, name)) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "discard.not_editing", name));
            return;
        }

        this.gameManager.getShopNpcManager().removeEditorNpcs(name);
        this.editorManager.endSession(player);

        // Descarta as alterações de blocos recriando o mundo a partir do schematic salvo.
        if (this.arenaManager.getMapFile(arena) != null) {
            this.arenaManager.resetArenaMap(name);
        }

        player.setGameMode(GameMode.SURVIVAL);
        final Location lobby = this.configManager.getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.YELLOW, "no_lobby"));
        }
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "discard.success", name));
    }

}
