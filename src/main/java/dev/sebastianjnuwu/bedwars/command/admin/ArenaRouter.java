package dev.sebastianjnuwu.bedwars.command.admin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.command.admin.config.SetCountdownCommand;
import dev.sebastianjnuwu.bedwars.command.admin.config.SetMinPlayersCommand;
import dev.sebastianjnuwu.bedwars.command.admin.config.ShopNpcCommand;
import dev.sebastianjnuwu.bedwars.command.admin.config.SpawnCommand;
import dev.sebastianjnuwu.bedwars.command.admin.config.StatusCommand;
import dev.sebastianjnuwu.bedwars.command.admin.generator.GeneratorAddCommand;
import dev.sebastianjnuwu.bedwars.command.admin.generator.GeneratorRemoveCommand;
import dev.sebastianjnuwu.bedwars.command.admin.team.SetBedCommand;
import dev.sebastianjnuwu.bedwars.command.admin.team.SetSpawnCommand;
import dev.sebastianjnuwu.bedwars.command.admin.team.TeamAddCommand;
import dev.sebastianjnuwu.bedwars.command.admin.team.TeamListCommand;
import dev.sebastianjnuwu.bedwars.command.admin.team.TeamRemoveCommand;
import dev.sebastianjnuwu.bedwars.command.admin.validator.ArenaEditorValidator;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Roteador de subcomandos para configuração de uma arena específica.
 * <p>
 * Uso: {@code /bw admin arena <arena> <açao> [argumentos...]}<br>
 * Valida se o jogador está editando a arena correta antes de delegar a
 * execução ao {@link ArenaSubCommand} apropriado. Ações suportadas:
 * {@code spawn}, {@code status}, {@code setminplayers}, {@code setcountdown},
 * {@code addteam}, {@code setspawn}, {@code setbed}, {@code teams},
 * {@code addgenerator}.
 * </p>
 *
 * @see BaseCommand
 * @see SubCommand
 * @see ArenaSubCommand
 */
public class ArenaRouter extends BaseCommand implements SubCommand {

    private final Map<String, ArenaSubCommand> subcommands = new LinkedHashMap<>();
    private final ArenaEditorValidator validator;

    /**
     * Construtor que inicializa o validador e registra todos os subcomandos
     * de configuração de arena.
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param editorManager gerenciador de edição de sessão (não nulo)
     * @param configManager gerenciador de configuração (não nulo)
     * @param gameManager   gerenciador da lógica do jogo (não nulo)
     * @param lang          gerenciador de internacionalização (não nulo)
     * @param mapsFolder    diretório de mapas (não nulo)
     */
    public ArenaRouter(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.validator = new ArenaEditorValidator(arenaManager, editorManager, lang);
        this.register("spawn", new SpawnCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("status", new StatusCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setminplayers", new SetMinPlayersCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setcountdown", new SetCountdownCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("addteam", new TeamAddCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("removeteam", new TeamRemoveCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setspawn", new SetSpawnCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("setbed", new SetBedCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("teams", new TeamListCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("addgenerator", new GeneratorAddCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("removegenerator", new GeneratorRemoveCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
        this.register("shop-npc", new ShopNpcCommand(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder));
    }

    /**
     * Executa o roteamento do comando de arena.
     * <p>
     * Exige pelo menos 3 argumentos. Verifica se o remetente é um {@link Player}.
     * Valida se o jogador tem permissão para editar a arena informada (através
     * de {@link ArenaEditorValidator}). Se a ação solicitada não estiver
     * registrada, envia uma mensagem de erro. Caso contrário, delega a execução
     * ao {@link ArenaSubCommand} correspondente.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um jogador)
     * @param args   argumentos do comando; espera-se {@code args[1]} como nome
     *               da arena e {@code args[2]} como ação (não nulo)
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 3) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.usage"));
            return;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        final Arena arena = this.validator.validate(player, args[1]);
        if (arena == null) {
            return;
        }

        final ArenaSubCommand cmd = this.subcommands.get(args[2].toLowerCase());
        if (cmd == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.unknown", args[2]));
            return;
        }
        cmd.execute(sender, arena, args);
    }

    /**
     * Registra um subcomando de arena no mapa interno.
     *
     * @param name o nome da ação (chave para lookup, não nulo)
     * @param cmd  a implementação do subcomando de arena (não nulo)
     */
    private void register(final String name, final ArenaSubCommand cmd) {
        this.subcommands.put(name, cmd);
    }
}
