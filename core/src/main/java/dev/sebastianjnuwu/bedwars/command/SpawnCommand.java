package dev.sebastianjnuwu.bedwars.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;

public class SpawnCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final ConfigManager configManager;

    public SpawnCommand(final GameManager gameManager, final ConfigManager configManager) {
        this.gameManager = gameManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(
            final @NotNull CommandSender sender,
            final @NotNull Command command,
            final @NotNull String label,
            final @NotNull String @NotNull [] args
    ) {
        if (!(sender instanceof final Player player)) {
            sender.sendMessage("Apenas jogadores podem usar este comando.");
            return true;
        }

        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }

        final var lobby = this.configManager.getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        }
        return true;
    }
}
