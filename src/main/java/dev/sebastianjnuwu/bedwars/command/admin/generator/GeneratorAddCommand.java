package dev.sebastianjnuwu.bedwars.command.admin.generator;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class GeneratorAddCommand extends BaseCommand implements ArenaSubCommand {

    public GeneratorAddCommand(
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
        final Player player = (Player) sender;
        if (args.length < 4) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_usage"));
            return;
        }
        final String rawType = args[3].toLowerCase();
        final String type = switch (rawType) {
            case "ferro" -> "iron";
            case "ouro" -> "gold";
            case "diamante", "diamond" -> "diamond";
            case "esmeralda", "emerald" -> "emerald";
            case "fornalha", "forja" -> "forge";
            default -> rawType;
        };
        if (!List.of("iron", "gold", "diamond", "emerald", "forge").contains(type)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_invalid"));
            return;
        }

        String teamName = null;
        if (type.equals("forge")) {
            if (args.length < 5) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_forge_usage"));
                return;
            }
            teamName = args[4].toLowerCase();
            final ArenaTeam team = arena.getTeam(teamName);
            if (team == null) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_forge_team_notfound", teamName));
                return;
            }
            final boolean alreadyHasForge = arena.getGenerators().stream()
                    .anyMatch(generator -> generator.getType().equalsIgnoreCase("forge")
                            && team.getName().equalsIgnoreCase(generator.getTeam()));
            if (alreadyHasForge) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_forge_duplicate", team.getName()));
                return;
            }
        }

        // Gerador fica no bloco embaixo do player, marcador também fica lá
        final Location loc = player.getLocation().getBlock().getRelative(0, -1, 0).getLocation();
        for (final var gen : arena.getGenerators()) {
            if (gen.getLocation() != null && gen.getLocation().equals(loc)) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_duplicate"));
                return;
            }
        }

        final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(type, loc);
        if (teamName != null) {
            gen.setTeam(teamName);
        }
        final var markerBlock = loc.getBlock();
        if (gen.getOriginBlockData() == null) {
            gen.setOriginBlockData(markerBlock.getBlockData().getAsString());
        }
        final Material marker = switch (type) {
            case "diamond" -> Material.DIAMOND_ORE;
            case "emerald" -> Material.EMERALD_ORE;
            case "gold"    -> Material.GOLD_ORE;
            case "iron"    -> Material.IRON_ORE;
            case "forge"   -> Material.BLAST_FURNACE;
            default        -> Material.SPONGE;
        };
        markerBlock.setType(marker);

        arena.addGenerator(gen);
        this.arenaManager.save(arena);
        
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.addgen_success", type));
    }
}
