package dev.sebastianjnuwu.bedwars.command;

import java.io.File;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Comando para um jogador sair da arena atual do BedWars.
 * <p>
 * Uso: {@code /bw leave}<br>
 * Remove o jogador do jogo em que ele está atualmente e envia uma
 * mensagem de confirmação.
 * </p>
 *
 * @see BaseCommand
 */
public class LeaveCommand extends BaseCommand {

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
    public LeaveCommand(
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
     * Executa a saída do jogador da arena atual.
     * <p>
     * Verifica se o remetente é um {@link Player}. Se não for, envia uma
     * mensagem de erro. Caso contrário, chama {@link GameManager#leaveGame(Player)}
     * e envia uma mensagem de sucesso.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um jogador para funcionar)
     */
    public void execute(final CommandSender sender) {
        if (!(sender instanceof final Player player)) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        if (!this.gameManager.isInGame(player)) {
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.not_in_game"));
            return;
        }
        this.gameManager.leaveGame(player);
        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "game.leave_success"));
    }
}
