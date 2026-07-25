package dev.sebastianjnuwu.bedwars.command.admin.config;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Comando para definir o lobby do servidor BedWars.
 * <p>
 * Define a localização atual do jogador como o ponto de spawn do lobby.
 * Esta localização é usada para teleportar jogadores antes das partidas
 * e após o salvamento de uma arena.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin setlobby}
 * </p>
 *
 * <p><b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada
 * apenas na thread principal do servidor.</p>
 */
public class SetLobbyCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de definição de lobby.
     *
     * @param arenaManager  gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager   gerenciador do jogo, não pode ser nulo
     * @param lang          gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder    diretório onde os arquivos de mapa são armazenados, não pode ser nulo
     */
    public SetLobbyCommand(
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
     * Executa o comando de definição de lobby.
     * <p>
     * Verifica se o remetente é um jogador e, em caso positivo, define
     * a localização atual como lobby através do {@link ConfigManager}.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param args   argumentos do comando (não utilizados)
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.setlobby.only_player"));
            return;
        }
        this.configManager.setLobby(player.getLocation());
        sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.setlobby.success",
                String.valueOf(player.getLocation().getBlockX()),
                String.valueOf(player.getLocation().getBlockY()),
                String.valueOf(player.getLocation().getBlockZ())));
    }
}
