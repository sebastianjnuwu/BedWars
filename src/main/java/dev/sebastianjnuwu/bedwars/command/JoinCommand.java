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
     * correto. Se um terceiro argumento for fornecido, ele é interpretado como
     * {@link ArenaMode} quando reconhecível; caso contrário, como o nome do time
     * desejado; quando nenhum é fornecido, a seleção é automática em partida livre.
     * </p>
     *
     * @param sender o remetente do comando (pode ser console)
     * @param args   argumentos do comando; espera-se {@code args[1]} como nome da arena
     *               e opcionalmente {@code args[2]} como modo ou nome do time (não nulo)
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
        if (args.length >= 3) {
            final ArenaMode mode = ArenaMode.fromAlias(args[2]);
            if (mode != null) {
                this.gameManager.joinGame(player, args[1], mode);
            } else {
                this.gameManager.joinGame(player, args[1], args[2]);
            }
        } else {
            this.gameManager.joinGame(player, args[1], (String) null);
        }
    }
}
