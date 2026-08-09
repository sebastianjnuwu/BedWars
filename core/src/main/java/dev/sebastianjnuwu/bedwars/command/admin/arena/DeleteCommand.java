package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.command.CommandSender;
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
 * Comando para deletar uma arena existente do BedWars.
 * <p>
 * Remove a arena do gerenciador de arenas pelo nome. Exibe mensagens de
 * erro caso a arena não seja encontrada.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena delete <nome>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class DeleteCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de exclusão de arena.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public DeleteCommand(
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
     * Executa o comando de exclusão de arena.
     * <p>
     * Valida se o nome da arena foi fornecido e se a arena existe.
     * Em caso positivo, remove a arena do {@link ArenaManager}.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "delete.usage"));
            return;
        }
        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);
        if (arena == null) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "delete.not_found", name));
            return;
        }
        this.arenaManager.delete(name);
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "delete.success", name));
    }
}
