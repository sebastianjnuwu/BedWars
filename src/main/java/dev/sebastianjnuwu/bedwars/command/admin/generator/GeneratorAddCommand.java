package dev.sebastianjnuwu.bedwars.command.admin.generator;

import dev.sebastianjnuwu.bedwars.command.BaseCommand;
import dev.sebastianjnuwu.bedwars.command.admin.ArenaSubCommand;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
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
        final String type = args[3].toLowerCase();
        if (!List.of("bronze", "ferro", "ouro", "forge").contains(type)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_invalid"));
            return;
        }
        final Location loc = player.getLocation();
        for (final var gen : arena.getGenerators()) {
            if (gen.getLocation().equals(loc)) {
                player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.addgen_duplicate"));
                return;
            }
        }
        final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(type, loc);
        final var below = loc.getBlock().getRelative(0, -1, 0);
        gen.setOriginBlockData(below.getBlockData().getAsString());
        below.setType(Material.SPONGE);
        final var above = loc.getBlock().getRelative(0, 1, 0);
        gen.setOriginBlockDataAbove(above.getBlockData().getAsString());
        if (type.equals("forge")) {
            above.setType(Material.FURNACE);
            this.createForgeHologram(loc);
        } else if (type.equals("ouro")) {
            above.setType(Material.GOLD_BLOCK);
        } else if (type.equals("ferro")) {
            above.setType(Material.IRON_BLOCK);
        } else {
            above.setType(Material.BRICK);
        }
        arena.addGenerator(gen);
        this.arenaManager.save(arena);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "admin.arena.addgen_success", type));
    }

    private void createForgeHologram(final Location location) {
        location.getWorld().spawn(location.clone().add(0.5, 2.2, 0.5), ArmorStand.class, stand -> {
            stand.setInvisible(true);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.addScoreboardTag("bedwars_forge_hologram");
            stand.customName(net.kyori.adventure.text.Component.text("Forja - Nivel 1", NamedTextColor.GOLD));
            stand.setCustomNameVisible(true);
        });
    }
}
