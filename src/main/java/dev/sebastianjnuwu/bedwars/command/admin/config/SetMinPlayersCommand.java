package dev.sebastianjnuwu.bedwars.command.admin.config;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Comando para definir o número mínimo de jogadores de uma arena.
 * <p>
 * Define a quantidade mínima de jogadores necessária para iniciar uma
 * partida. O valor mínimo aceito é 1.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena setminplayers <arena> <quantidade>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class SetMinPlayersCommand extends BaseCommand implements ArenaSubCommand {

    /**
     * Construtor do comando de definição de mínimo de jogadores.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public SetMinPlayersCommand(
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
     * Executa o comando de definição de mínimo de jogadores.
     * <p>
     * Lê o valor inteiro do argumento {@code args[3]}, valida se é
     * maior ou igual a 1 e atualiza a arena. Persiste a alteração
     * no {@link ArenaManager}.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param arena  a arena alvo da configuração
     * @param args   argumentos do comando; {@code args[3]} deve conter a quantidade mínima
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull Arena arena, final @NotNull String @NotNull [] args) {
        final Player player = (Player) sender;
        if (args.length < 4) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setminplayers_usage"));
            return;
        }
        try {
            final int min = Integer.parseInt(args[3]);
            if (min < 1) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setminplayers_invalid"));
                return;
            }
            arena.setMinPlayers(min);
            this.arenaManager.save(arena);
            player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.setminplayers_success", String.valueOf(min)));
        } catch (final NumberFormatException e) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setminplayers_invalid"));
        }
    }
}
