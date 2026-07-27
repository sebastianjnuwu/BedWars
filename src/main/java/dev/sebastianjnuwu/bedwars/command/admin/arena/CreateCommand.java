package dev.sebastianjnuwu.bedwars.command.admin.arena;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.SubCommand;
import dev.sebastianjnuwu.bedwars.editor.ArenaCreator;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;
import dev.sebastianjnuwu.bedwars.template.TemplateManager;
import dev.sebastianjnuwu.bedwars.world.VoidGenerator;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Comando /bw admin create <nome>
 * <p>
 * Cria uma nova arena com mundo void para edição.
 * O administrador entra no mundo e constrói usando FAWE.
 * Após a construção, o template é salvo com /bw admin save.
 * </p>
 */
public class CreateCommand extends BaseCommand implements SubCommand {

    private final SlimeManager slimeManager;
    private final TemplateManager templateManager;
    private final ArenaCreator arenaCreator;

    public CreateCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
        
        // Inicializa SlimeManager
        final var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("BedWars");
        this.slimeManager = new SlimeManager(plugin);
        this.templateManager = new TemplateManager(this.slimeManager.getTemplatesFolder(), this.slimeManager);
        this.arenaCreator = new ArenaCreator(this.slimeManager, plugin.getDataFolder(), mapsFolder, this.slimeManager.getTemplatesFolder());
    }

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

        // Verifica se o template já existe
        if (this.templateManager.templateExists(name)) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.template_exists", name));
            return;
        }

        // Cria o mundo de edição void
        final String worldName;
        if (this.slimeManager.isAvailable()) {
            worldName = this.arenaCreator.createEditWorld(name, player);
        } else {
            worldName = createFallbackWorld(name, player);
        }
        if (worldName == null) {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.error", "Falha ao criar mundo de edição"));
            return;
        }

        // Cria a arena no gerenciador
        final Arena arena = this.arenaManager.create(name);
        if (arena == null) {
            this.arenaCreator.deleteEditWorld(worldName);
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "create.already_exists", name));
            return;
        }

        arena.setWorldName(worldName);
        arena.setEnabled(false); // Arena só é habilitada após salvar o template
        arena.setMinPlayers(2);
        arena.setCountdown(3);

        // Salva a arena
        this.arenaManager.save(arena);

        sender.sendMessage(this.lang.text(
                NamedTextColor.GREEN,
                "create.success_void",
                name,
                worldName
        ));
        sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "create.instructions"));

        if (!this.configManager.hasLobby()) {
            sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "no_lobby"));
        }
    }

    private @Nullable String createFallbackWorld(@NotNull String arenaName, @NotNull Player creator) {
        final String worldName = "bw_edit_" + arenaName + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);

        final WorldCreator wc = new WorldCreator(worldName);
        wc.generator(new VoidGenerator());
        wc.generateStructures(false);
        wc.environment(World.Environment.NORMAL);

        final World world = wc.createWorld();
        if (world == null) {
            return null;
        }

        world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        world.setTime(1000);
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);

        createPlatform(world);
        creator.teleport(new org.bukkit.Location(world, 0.5, 2, 0.5));
        return worldName;
    }

    private void createPlatform(@NotNull World world) {
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                world.getBlockAt(x, 0, z).setType(org.bukkit.Material.GLASS);
            }
        }
    }
}
