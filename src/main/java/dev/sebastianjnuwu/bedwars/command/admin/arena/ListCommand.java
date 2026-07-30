package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando para listar todas as arenas registradas.
 * <p>
 * Exibe o nome de cada arena e seu status (arquivo .bwmap presente ou ausente).
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena list}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class ListCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de listagem de arenas.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public ListCommand(
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
     * Executa o comando de listagem de arenas.
     * <p>
     * Obtém todos os nomes de arena do {@link ArenaManager}. Para cada
     * arena, verifica a existência do arquivo .bwmap correspondente e
     * exibe o nome com o respectivo status.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando (não utilizados na listagem)
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        final var names = this.arenaManager.getNames();
        if (names.isEmpty()) {
            sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "list.empty"));
            return;
        }
        sender.sendMessage(this.lang.text(NamedTextColor.GOLD, "list.header", String.valueOf(names.size())));
        for (final String name : names) {
            final boolean hasFile = new File(this.mapsFolder, name + ".bwmap").exists();
            final var arena = this.arenaManager.get(name);
            
            final String status;
            if (!hasFile) {
                status = this.lang.raw("list.status_missing_map");
            } else if (arena == null) {
                status = this.lang.raw("list.error");
            } else {
                final java.util.List<String> missing = this.gameManager.validateArena(arena);
                if (missing.isEmpty()) {
                    status = this.lang.raw("list.status_ready");
                } else {
                    status = this.lang.raw("list.status_incomplete");
                }
            }
            sender.sendMessage(this.lang.text("list.entry", name, status));
        }
    }
}
