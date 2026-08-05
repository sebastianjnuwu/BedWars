package dev.sebastianjnuwu.bedwars.command;

import java.io.File;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando para iniciar uma partida em uma arena do BedWars.
 * <p>
 * Uso: {@code /bw start <arena>}<br>
 * Valida se a arena existe e está pronta para iniciar (todos os requisitos
 * configurados). Se estiver pronta, a partida é iniciada. Caso contrário,
 * exibe uma lista dos itens pendentes.
 * </p>
 *
 * @see BaseCommand
 */
public class StartCommand extends BaseCommand {

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
    public StartCommand(
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
     * Executa a lógica de início de partida.
     * <p>
     * Exige pelo menos dois argumentos. Se a arena informada não existir, envia
     * uma mensagem de erro. Se a arena existir mas não estiver pronta (faltam
     * configurações obrigatórias), exibe a lista de itens pendentes e aborta.
     * Caso tudo esteja ok, delega ao {@link GameManager#startGame(String)}.
     * </p>
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; espera-se {@code args[1]} como nome da arena
     *               (não nulo)
     */
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.start_usage"));
            return;
        }
        final String arenaName = args[1];
        final Arena arena = this.arenaManager.get(arenaName);
        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.arena_not_found", arenaName));
            return;
        }
        if (this.arenaManager.ensureWorldLoaded(arena) == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.world_not_ready", arenaName));
            return;
        }
        final Arena refreshed = this.arenaManager.get(arenaName);
        final List<String> missing = this.gameManager.validateArena(refreshed);
        if (!missing.isEmpty()) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "game.not_ready", arenaName));
            for (final String msg : missing) {
                sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "game.missing_entry", msg));
            }
            return;
        }
        this.gameManager.joinGame(player, arenaName);
        if (this.gameManager.isInGame(player)) {
            this.gameManager.startGame(arenaName);
        }
    }
}
