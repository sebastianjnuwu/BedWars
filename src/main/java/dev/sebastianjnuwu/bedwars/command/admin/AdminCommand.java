package dev.sebastianjnuwu.bedwars.command.admin;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.command.admin.arena.LifecycleRouter;
import dev.sebastianjnuwu.bedwars.command.admin.arena.SetLobbyCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ReloadCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Comando administrativo do BedWars (<b>/bw admin</b>).
 * <p>
 * Gerencia um conjunto de subcomandos administrativos registrados em um
 * {@link java.util.LinkedHashMap}. Roteia a execução para o subcomando
 * correto com base no segundo argumento. Subcomandos registrados:
 * {@code arena}, {@code setlobby}, {@code create}, {@code delete},
 * {@code list}, {@code save}, {@code load}, {@code edit}.
 * </p><p>
 * <b>Exemplo de uso:</b><pre>{@code
 * /bw admin create minha_arena
 * /bw admin arena minha_arena spawn
 * /bw admin setlobby
 * }</pre>
 * </p>
 *
 * @see BaseCommand
 * @see ArenaRouter
 */
public class AdminCommand extends BaseCommand {

    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    /**
     * Construtor que inicializa e registra todos os subcomandos administrativos.
     * <p>
     * Cria instâncias de {@link LifecycleRouter}, {@link ArenaRouter} e
     * {@link SetLobbyCommand} e as registra no mapa de subcomandos.
     * </p>
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param editorManager gerenciador de edição de sessão (não nulo)
     * @param configManager gerenciador de configuração (não nulo)
     * @param gameManager   gerenciador da lógica do jogo (não nulo)
     * @param lang          gerenciador de internacionalização (não nulo)
     * @param mapsFolder    diretório de mapas (não nulo)
     */
    public AdminCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        final LifecycleRouter lifecycle = new LifecycleRouter(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.register("arena", new ArenaRouter(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setlobby", new SetLobbyCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("create", lifecycle);
        this.register("delete", lifecycle);
        this.register("list", lifecycle);
        this.register("save", lifecycle);
        this.register("load", lifecycle);
        this.register("edit", lifecycle);
        this.register("reload", new ReloadCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
    }

    /**
     * Executa o comando administrativo.
     * <p>
     * Exige pelo menos dois argumentos. O segundo argumento ({@code args[1]})
     * identifica o subcomando (case-insensitive). Se o subcomando não estiver
     * registrado, uma mensagem de erro é enviada. Os argumentos a partir do
     * segundo são repassados ao subcomando.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; espera-se {@code args[1]} como nome
     *               do subcomando (não nulo)
     */
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.usage"));
            return;
        }
        final SubCommand cmd = this.subcommands.get(args[1].toLowerCase());
        if (cmd == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.unknown", args[1]));
            return;
        }
        cmd.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Registra um subcomando administrativo no mapa interno.
     *
     * @param name o nome do subcomando (chave para lookup, não nulo)
     * @param cmd  a implementação do subcomando (não nulo)
     */
    private void register(final String name, final SubCommand cmd) {
        this.subcommands.put(name, cmd);
    }
}
