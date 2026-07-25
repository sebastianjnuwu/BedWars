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
        sender.sendMessage(Component.text("Minimo de jogadores: " + arena.getMinPlayers(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Contagem regressiva: " + arena.getCountdown() + "s", NamedTextColor.WHITE));
        final long forgeCount = arena.getGenerators().stream()
                .filter(generator -> generator.getType().equalsIgnoreCase("forge"))
                .count();
        sender.sendMessage(Component.text("Forjas configuradas: " + forgeCount, NamedTextColor.WHITE));
        for (int level = 1; level <= this.configManager.getForgeMaxLevel(); level++) {
            final Map<Material, Long> intervals = this.configManager.getForgeIntervals(level);
            if (intervals.isEmpty()) continue;
            final String entries = intervals.entrySet().stream()
                    .map(entry -> this.displayName(entry.getKey()) + ": " + entry.getValue() + "t")
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("");
            sender.sendMessage(Component.text("Forja nivel " + level + ": " + entries, NamedTextColor.GRAY));
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
