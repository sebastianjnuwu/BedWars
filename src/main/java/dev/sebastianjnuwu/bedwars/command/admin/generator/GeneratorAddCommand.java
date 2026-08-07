package dev.sebastianjnuwu.bedwars.command.admin.generator;

import java.io.File;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

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
        }

        // Gerador fica no bloco embaixo do player, marcador também fica lá
        final Location loc = player.getLocation().getBlock().getRelative(0, -1, 0).getLocation();

        // Remove gerador existente na mesma posição ou fornalha do mesmo time (sobrescreve)
        final String targetTeam = teamName;
        final var existing = arena.getGenerators().stream()
                .filter(g -> {
                    if (type.equals("forge") && g.getType().equalsIgnoreCase("forge")
                            && targetTeam != null && g.getTeam() != null
                            && g.getTeam().equalsIgnoreCase(targetTeam)) {
                        return true;
                    }
                    return g.getLocation() != null && g.getLocation().equals(loc);
                })
                .findFirst().orElse(null);
        if (existing != null && existing.getLocation() != null) {
            final var oldMarker = existing.getLocation().getBlock();
            if (existing.getOriginBlockData() != null) {
                oldMarker.setBlockData(org.bukkit.Bukkit.createBlockData(existing.getOriginBlockData()), false);
            } else {
                oldMarker.setType(Material.AIR, false);
            }
            arena.getGenerators().remove(existing);
        }

        final var gen = new ArenaGenerator(type, loc);
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
