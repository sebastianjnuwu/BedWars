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
 * Comando para definir o tempo de contagem regressiva de uma arena.
 * <p>
 * Permite configurar a duração da contagem regressiva antes do início
 * da partida. O valor mínimo aceito é de 3 segundos.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena setcountdown <arena> <segundos>}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class SetCountdownCommand extends BaseCommand implements ArenaSubCommand {

    /**
     * Construtor do comando de definição de contagem regressiva.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public SetCountdownCommand(
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
     * Executa o comando de definição de contagem regressiva.
     * <p>
     * Lê o valor em segundos do argumento {@code args[3]}, valida se é
     * um número inteiro maior ou igual a 3 e atualiza a arena. Persiste
     * a alteração no {@link ArenaManager}.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param arena  a arena alvo da configuração
     * @param args   argumentos do comando; {@code args[3]} deve conter a quantidade de segundos
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull Arena arena, final @NotNull String @NotNull [] args) {
        final Player player = (Player) sender;
        if (args.length < 4) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setcountdown_usage"));
            return;
        }
        try {
            final int seconds = Integer.parseInt(args[3]);
            if (seconds < 3) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setcountdown_invalid"));
                return;
            }
            arena.setCountdown(seconds);
            this.arenaManager.save(arena);
            player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.setcountdown_success", String.valueOf(seconds)));
        } catch (final NumberFormatException e) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.setcountdown_invalid"));
        }
    }
}
