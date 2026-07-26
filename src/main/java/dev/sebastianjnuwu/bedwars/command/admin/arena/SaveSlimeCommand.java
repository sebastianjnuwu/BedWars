package dev.sebastianjnuwu.bedwars.command.admin.arena;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Comando para salvar um mundo como template Slime.
 * <p>
 * Exemplo de uso: {@code /bwadmin arena saveslime <nome>}
 * </p>
 */
public class SaveSlimeCommand extends BaseCommand implements SubCommand {

    private final SlimeManager slimeManager;

    /**
     * Construtor do comando de salvamento como template Slime.
     *
     * @param arenaManager  gerenciador de arenas
     * @param editorManager gerenciador de sessões de edição
     * @param configManager gerenciador de configurações
     * @param gameManager   gerenciador do jogo
     * @param lang          gerenciador de internacionalização
     * @param mapsFolder    diretório de mapas
     * @param slimeManager  gerenciador de SlimeWorld
     */
    public SaveSlimeCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder,
            final SlimeManager slimeManager
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        this.slimeManager = slimeManager;
    }

    /**
     * Executa o comando de salvamento como template Slime.
     *
     * @param sender o remetente do comando
     * @param args   argumentos do comando; {@code args[1]} deve conter o nome da arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.usage"));
            return;
        }

        final String name = args[1];
        final Arena arena = this.arenaManager.get(name);

        if (arena == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.not_found", name));
            return;
        }

        final String worldName = arena.getWorldName();
        if (worldName == null || worldName.isBlank()) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.not_loaded", name));
            return;
        }

        final World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.world_not_found", worldName));
            return;
        }

        // Verifica se o SlimeManager está disponível
        if (!this.slimeManager.isAvailable()) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.slime_not_available"));
            return;
        }

        // Salva como template
        sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "saveslime.saving", name));

        this.slimeManager.saveTemplate(name, world)
                .thenRun(() -> {
                    sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "saveslime.success", name));
                    sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "saveslime.now_use_slime"));

                    // Remove o marker blocks
                    if (sender instanceof Player player) {
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        final var lobby = this.configManager.getLobby();
                        if (lobby != null) {
                            player.teleport(lobby);
                        }
                    }
                })
                .exceptionally(ex -> {
                    sender.sendMessage(this.lang.text(NamedTextColor.RED, "saveslime.error", ex.getMessage()));
                    return null;
                });
    }
}
