package dev.sebastianjnuwu.bedwars.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.admin.AdminCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando principal do BedWars (<b>/bw</b>).
 * <p>
 * Implementa {@link CommandExecutor} e {@link TabCompleter} para rotear
 * subcomandos como "admin", "join", "leave" e "start". Delega a execução
 * para instâncias especializadas ({@link JoinCommand}, {@link LeaveCommand},
 * {@link StartCommand} e {@link AdminCommand}) e fornece sugestões de
 * tab-completion contextuais.
 * </p><p>
 * <b>Exemplo de uso:</b><pre>{@code
 * /bw join minha_arena azul
 * /bw admin create nova_arena
 * /bw leave
 * }</pre>
 * </p>
 *
 * @see JoinCommand
 * @see LeaveCommand
 * @see StartCommand
 * @see AdminCommand
 */
public class BWCommand implements CommandExecutor, TabCompleter {

    private final JoinCommand joinCommand;
    private final LeaveCommand leaveCommand;
    private final StartCommand startCommand;
    private final AdminCommand adminCommand;
    private final LangManager lang;
    private final ArenaManager arenaManager;

    /**
     * Construtor que cria todas as instâncias de subcomandos com as dependências fornecidas.
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param editorManager gerenciador de edição de sessão (não nulo)
     * @param configManager gerenciador de configuração (não nulo)
     * @param gameManager   gerenciador da lógica do jogo (não nulo)
     * @param lang          gerenciador de internacionalização (não nulo)
     * @param mapsFolder    diretório de mapas (não nulo)
     */
    public BWCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        this.lang = lang;
        this.arenaManager = arenaManager;
        this.joinCommand = new JoinCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.leaveCommand = new LeaveCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.startCommand = new StartCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.adminCommand = new AdminCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
    }

    /**
     * Executa o comando principal.
     * <p>
     * Se nenhum argumento for fornecido, envia uma mensagem de uso. Caso contrário,
     * roteia para o subcomando correspondente com base no primeiro argumento
     * (case-insensitive). Subcomandos reconhecidos: {@code admin}, {@code join},
     * {@code leave} e {@code start}. Para qualquer outro valor, exibe uma mensagem
     * de subcomando desconhecido.
     * </p>
     *
     * @param sender  a entidade que enviou o comando (não nulo)
     * @param command o objeto do comando executado (não nulo)
     * @param label   o rótulo do comando usado (não nulo)
     * @param args    os argumentos do comando (não nulo, pode ser vazio)
     * @return {@code true} sempre, indicando que o comando foi processado
     */
    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String @NotNull [] args
    ) {
        if (args.length == 0) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "admin":
                this.adminCommand.execute(sender, args);
                break;
            case "join":
                this.joinCommand.execute(sender, args);
                break;
            case "leave":
                this.leaveCommand.execute(sender);
                break;
            case "start":
                this.startCommand.execute(sender, args);
                break;
            default:
                sender.sendMessage(
                        this.lang.text(NamedTextColor.RED, "unknown_subcommand", args[0]));
                break;
        }
        return true;
    }

    /**
     * Fornece sugestões de tab-completion contextuais.
     * <p>
     * As sugestões variam conforme a profundidade dos argumentos:
     * <ul>
     *   <li>1 argumento: {@code admin}, {@code join}, {@code leave}, {@code start}</li>
     *   <li>2 argumentos: subcomandos do admin ou nomes de arena para join/start</li>
     *   <li>3 argumentos: times da arena (join) ou operações admin/arena</li>
     *   <li>4 argumentos: ações da arena ({@code spawn}, {@code addteam}, etc.)</li>
     *   <li>5 argumentos: nomes de times, tipos de gerador ou as ações do
     *       subcomando {@code shop-npc} ({@code add}, {@code remove}, {@code list}, {@code displayname})</li>
     * </ul>
     * Retorna uma lista vazia se nenhuma sugestão for aplicável.
     * </p>
     *
     * @param sender  a entidade que solicitou a complementação (não nulo)
     * @param command o objeto do comando (não nulo)
     * @param label   o rótulo do comando (não nulo)
     * @param args    os argumentos atuais (não nulo, pode ser vazio)
     * @return lista de Strings com as sugestões de completação (nunca nula)
     */
    @Override
    public List<String> onTabComplete(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String @NotNull [] args
    ) {
        final List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("admin", "join", "leave", "start"));
        } else if (args.length == 2) {
            final String first = args[0].toLowerCase();
            if (first.equals("admin")) {
                completions.addAll(List.of("create", "delete", "list", "save", "load", "edit", "setlobby", "arena", "reload"));
            } else if (List.of("join", "start").contains(first)) {
                completions.addAll(this.arenaManager.getNames());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("join")) {
            final Arena teamArena = this.arenaManager.get(args[1]);
            if (teamArena != null) {
                for (final ArenaTeam at : teamArena.getTeams()) {
                    completions.add(at.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
            final String sub = args[1].toLowerCase();
            switch (sub) {
                case "delete", "save", "load", "edit":
                    completions.addAll(this.arenaManager.getNames());
                    break;
                case "arena":
                    completions.addAll(this.arenaManager.getNames());
                    break;
                default:
                    break;
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("arena")) {
            completions.addAll(List.of("spawn", "addteam", "removeteam", "setspawn", "setbed", "addgenerator", "teams", "status", "setminplayers", "setcountdown", "shop-npc"));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("arena")) {
            final String sub = args[3].toLowerCase();
            if (List.of("addteam", "removeteam", "setspawn", "setbed").contains(sub)) {
                completions.addAll(List.of("azul", "vermelho", "verde", "amarelo", "roxo", "rosa", "laranja", "ciano"));
            } else if (sub.equals("addgenerator")) {
                completions.addAll(List.of("ferro", "ouro", "diamante", "esmeralda", "forge"));
            } else if (sub.equals("shop-npc")) {
                completions.addAll(List.of("add", "remove", "list", "displayname"));
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("arena")) {
            final String sub = args[3].toLowerCase();
            final String arenaName = args[2];
            if (sub.equals("addgenerator") && args[4].equalsIgnoreCase("forge")) {
                final Arena forgeArena = this.arenaManager.get(arenaName);
                if (forgeArena != null) {
                    for (final ArenaTeam at : forgeArena.getTeams()) {
                        completions.add(at.getName());
                    }
                }
            }
        }
        return completions;
    }
}
