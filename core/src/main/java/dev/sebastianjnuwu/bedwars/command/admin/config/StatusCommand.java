package dev.sebastianjnuwu.bedwars.command.admin.config;

import java.io.File;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

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
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GOLD, "admin.arena.status_header", arena.getName()));
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.WHITE, "admin.arena.status_team_limits",
                String.valueOf(arena.getMinPlayersPerTeam()),
                String.valueOf(arena.getMaxPlayersPerTeam() == 0 ? "modo" : arena.getMaxPlayersPerTeam()),
                String.valueOf(arena.getMinTeamsToStart())));
        final int teamCount = arena.getTeams().size();
        final String modes = java.util.Arrays.stream(ArenaMode.values())
                .filter(mode -> mode.isValidFor(teamCount))
                .map(mode -> mode.name().toLowerCase())
                .reduce((left, right) -> left + ", " + right)
                .orElse("—");
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.WHITE, "admin.arena.status_modes", String.valueOf(teamCount), modes));
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.WHITE, "admin.arena.status_countdown", String.valueOf(arena.getCountdown())));
        final long forgeCount = arena.getGenerators().stream()
                .filter(generator -> generator.getType().equalsIgnoreCase("forge"))
                .count();
        CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.WHITE, "admin.arena.status_forges", String.valueOf(forgeCount)));
        final List<ForgeLevel> arenaLevels = arena.getForgeLevels();
        if (arenaLevels != null) {
            for (final ForgeLevel fl : arenaLevels) {
                final String entries = fl.intervals().entrySet().stream()
                        .map(entry -> this.displayName(entry.getKey()) + ": " + entry.getValue() + "t")
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("");
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GRAY, "admin.arena.status_forge_level", String.valueOf(fl.level()), entries));
            }
        }
        if (missing.isEmpty()) {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.GREEN, "admin.arena.status_ready"));
        } else {
            CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.RED, "admin.arena.status_missing_header"));
            for (final String msg : missing) {
                CompatProvider.chat().sendMessage(sender, this.lang.text(NamedTextColor.YELLOW, "admin.arena.status_entry", msg));
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
