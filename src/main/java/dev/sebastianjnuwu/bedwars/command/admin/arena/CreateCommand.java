package dev.sebastianjnuwu.bedwars.command.admin.arena;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Comando para criar uma nova arena no BedWars.
 * <p>
 * Este comando utiliza a seleção do WorldEdit para definir o tamanho e a
 * posição inicial do schematic da arena. A arena é criada com um nome único,
 * salva no gerenciador de arenas e marcada como ativa.
 * </p>
 * <p>
 * Exemplo de uso: {@code /bwadmin arena create <nome>}
 * </p>
 *
 * <p>
 * <b>Thread safety:</b> Esta classe não é thread-safe. Deve ser usada apenas na
 * thread principal do servidor.</p>
 */
public class CreateCommand extends BaseCommand implements SubCommand {

    /**
     * Construtor do comando de criação de arena.
     *
     * @param arenaManager gerenciador de arenas, não pode ser nulo
     * @param editorManager gerenciador de sessões de edição, não pode ser nulo
     * @param configManager gerenciador de configurações, não pode ser nulo
     * @param gameManager gerenciador do jogo, não pode ser nulo
     * @param lang gerenciador de internacionalização, não pode ser nulo
     * @param mapsFolder diretório onde os arquivos de mapa (.bwmap) são
     * armazenados, não pode ser nulo
     */
    public CreateCommand(
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
     * Executa o comando de criação de arena.
     * <p>
     * Valida os argumentos, verifica se o jogador possui uma seleção do
     * WorldEdit, calcula as dimensões da arena e a registra no
     * {@link ArenaManager}.
     * </p>
     *
     * @param sender o remetente do comando (deve ser um {@link Player})
     * @param args argumentos do comando; {@code args[1]} deve conter o nome da
     * arena
     */
    @Override
    public void execute(final CommandSender sender, final @NotNull String @NotNull [] args) {
        if (args.length < 2) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.usage"));
            return;
        }
        if (!(sender instanceof final Player player)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.only_player"));
            return;
        }
        final String name = args[1];
        if (this.arenaManager.get(name) != null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.already_exists", name));
            return;
        }
        if (!this.configManager.hasLobby()) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "no_lobby"));
            return;
        }
        final WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        if (worldEdit == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.error", "WorldEdit nao encontrado."));
            return;
        }
        try {
            final LocalSession session = worldEdit.getSession(player);
            final var selection = session.getSelection(BukkitAdapter.adapt(player.getWorld()));
            final BlockVector3 min = selection.getMinimumPoint();
            final BlockVector3 max = selection.getMaximumPoint();
            final int width = max.x() - min.x() + 1;
            final int height = max.y() - min.y() + 1;
            final int length = max.z() - min.z() + 1;

            if (width > this.configManager.getConfig().getInt("arena.limits.max-width")
                    || height > this.configManager.getConfig().getInt("arena.limits.max-height")
                    || length > this.configManager.getConfig().getInt("arena.limits.max-length")) {

                sender.sendMessage(this.lang.text(
                        NamedTextColor.RED,
                        "create.too_large",
                        width + "x" + height + "x" + length
                ));
                return;
            }

            final Arena arena = this.arenaManager.create(name);

            if (arena == null) {
                sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.already_exists", name));
                return;
            }
            arena.setPaste(min.x(), min.y(), min.z());
            arena.setSchematicSize(width, height, length);
            arena.setWorldName("bw_" + name);
            arena.setEnabled(true);
            this.arenaManager.save(arena);
            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "create.success", name,
                    String.valueOf(width * height * length)));
        } catch (final IncompleteRegionException e) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.no_selection"));
        }
    }
}
