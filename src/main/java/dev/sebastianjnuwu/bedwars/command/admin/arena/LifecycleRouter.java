package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * Roteador de subcomandos do ciclo de vida de arenas.
 * <p>
 * Gerencia e encaminha comandos relacionados ao ciclo de vida de uma arena,
 * tais como criar, deletar, listar, salvar, carregar e editar. Utiliza um
 * mapa interno de subcomandos registrados no construtor para realizar o
 * despacho com base no primeiro argumento do comando.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena <create|delete|list|save|load|edit|discard> ...}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class LifecycleRouter extends BaseCommand implements SubCommand {

    private final Map<String, SubCommand> subcommands = new LinkedHashMap<>();

    /**
     * Construtor do roteador de ciclo de vida.
     * <p>
     * Registra todos os subcomandos disponíveis (create, delete, list, save,
     * load, edit) no mapa interno de despacho.
     * </p>
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public LifecycleRouter(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.register("create", new CreateCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("delete", new DeleteCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("list", new ListCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("save", new SaveCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("load", new LoadCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("edit", new EditCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("discard", new DiscardCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
    }

    /**
     * Executa o roteamento do comando para o subcomando apropriado.
     * <p>
     * O primeiro argumento ({@code args[0]}) é usado como chave para
     * localizar o subcomando registrado. Se nenhum subcomando for
     * encontrado, uma mensagem de erro é enviada ao remetente.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; {@code args[0]} deve conter o nome do subcomando
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        final SubCommand cmd = this.subcommands.get(args[0].toLowerCase());
        if (cmd == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.unknown", args[0]));
            return;
        }
        cmd.execute(sender, args);
    }

    /**
     * Registra um subcomando no mapa de despacho interno.
     *
     * @param name o nome do subcomando (chave de busca)
     * @param cmd  a implementação do subcomando a ser registrada
     */
    private void register(final String name, final SubCommand cmd) {
        this.subcommands.put(name, cmd);
    }
}
