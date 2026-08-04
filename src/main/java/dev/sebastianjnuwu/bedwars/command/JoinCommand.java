package dev.sebastianjnuwu.bedwars.command;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando para um jogador entrar em uma arena do BedWars.
 * <p>
 * Uso: {@code /bw join <arena> [modo|time]}<br>
 * O segundo argumento opcional é interpretado como {@link ArenaMode} quando
 * corresponde a um modo (ex.: {@code solo}, {@code dupla}, {@code trio},
 * {@code quarteto}); caso contrário, é tratado como o nome de um time. Se
 * omitido, o jogador é atribuído automaticamente a um time disponível de uma
 * partida livre.
 * </p>
 *
 * @see BaseCommand
 */
public class JoinCommand extends BaseCommand {

    /**
     * Construtor que repassa todas as dependências ao {@link BaseCommand}.
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param editorManager gerenciador de edição de sessão (não nulo)
     * @param configManager gerenciador de configuração (não nulo)
     * @param gameManager   gerenciador da lógica do jogo (não nulo)
     * @param lang          gerenciador de internacionalização (não nulo)
     * @param mapsFolder    diretório de mapas (não nulo)
     */
    public JoinCommand(
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
     * Executa a lógica de entrada na arena.
     * <p>
     * Verifica se o remetente é um {@link Player}. Se não for, envia uma mensagem
     * de erro. Se os argumentos forem insuficientes (menos de dois), exibe o uso
     * correto. O segundo argumento é o nome da arena. Os argumentos seguintes podem
     * usar as flags opcionais {@code --mode <modo>} e {@code --team <time>} (em
     * qualquer ordem). Como fallback, argumentos posicionais são interpretados como
     * {@link ArenaMode} quando reconhecíveis e como nome do time caso contrário.
     * Quando nenhum é fornecido, a seleção é automática em partida livre.
     * </p>
     *
     * @param sender o remetente do comando (pode ser console)
     * @param args   argumentos do comando; espera-se {@code args[1]} como nome da arena
     *               e opcionalmente flags {@code --mode} e {@code --team} (não nulo)
     */
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.join_usage"));
            return;
        }

        final String arenaName = args[1];
        String teamName = null;
        String modeArg = null;
        boolean positional = true;
        for (int i = 2; i < args.length; i++) {
            final String arg = args[i];
            if (arg.equalsIgnoreCase("--mode") || arg.equalsIgnoreCase("--modo")) {
                if (i + 1 < args.length) {
                    modeArg = args[++i];
                    positional = false;
                }
            } else if (arg.equalsIgnoreCase("--team") || arg.equalsIgnoreCase("--time")) {
                if (i + 1 < args.length) {
                    teamName = args[++i];
                    positional = false;
                }
            } else if (positional) {
                final ArenaMode mode = ArenaMode.fromAlias(arg);
                if (mode != null) {
                    modeArg = arg;
                } else {
                    teamName = arg;
                }
            }
        }

        final ArenaMode mode = modeArg != null ? ArenaMode.fromAlias(modeArg) : null;
        this.gameManager.joinGame(player, arenaName, teamName, mode, true);
    }
}
