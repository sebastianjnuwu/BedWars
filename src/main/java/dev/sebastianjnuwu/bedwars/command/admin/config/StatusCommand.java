package dev.sebastianjnuwu.bedwars.command.admin.config;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import java.io.File;
import java.util.List;
import java.util.Map;

public class StatusCommand extends BaseCommand implements ArenaSubCommand {

    public StatusCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        super(arenaManager, editorManager, configManager, gameManager, lang, mapsFolder);
    }

    @Override
    public void execute(final CommandSender sender, final @NotNull Arena arena, final @NotNull String @NotNull [] args) {
        final List<String> missing = this.gameManager.validateArena(arena);
        sender.sendMessage(this.lang.text(NamedTextColor.GOLD, "admin.arena.status_header", arena.getName()));
        sender.sendMessage(this.lang.text(NamedTextColor.WHITE, "admin.arena.status_minplayers", String.valueOf(arena.getMinPlayers())));
        sender.sendMessage(this.lang.text(NamedTextColor.WHITE, "admin.arena.status_countdown", String.valueOf(arena.getCountdown())));
        final long forgeCount = arena.getGenerators().stream()
                .filter(generator -> generator.getType().equalsIgnoreCase("forge"))
                .count();
        sender.sendMessage(this.lang.text(NamedTextColor.WHITE, "admin.arena.status_forges", String.valueOf(forgeCount)));
        final List<ForgeLevel> arenaLevels = arena.getForgeLevels();
        if (arenaLevels != null) {
            for (final ForgeLevel fl : arenaLevels) {
                final String entries = fl.intervals().entrySet().stream()
                        .map(entry -> this.displayName(entry.getKey()) + ": " + entry.getValue() + "t")
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                sender.sendMessage(this.lang.text(NamedTextColor.GRAY, "admin.arena.status_forge_level", String.valueOf(fl.level()), entries));
            }
        }
        if (missing.isEmpty()) {
            sender.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.status_ready"));
        } else {
            sender.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.status_missing_header"));
            for (final String msg : missing) {
                sender.sendMessage(this.lang.text(NamedTextColor.YELLOW, "admin.arena.status_entry", msg));
            }
        }
    }

    private String displayName(final Material material) {
        return switch (material) {
            case IRON_INGOT -> "Ferro";
            case GOLD_INGOT -> "Ouro";
            case DIAMOND -> "Diamante";
            case EMERALD -> "Esmeralda";
            default -> material.name();
        };
    }
}
