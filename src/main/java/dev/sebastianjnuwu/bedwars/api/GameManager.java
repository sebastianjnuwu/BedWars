package dev.sebastianjnuwu.bedwars.api;

import java.util.List;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.Game;

public interface GameManager {

    @Nullable Game getGame(@NotNull String arenaName);

    @Nullable Game getPlayerGame(@NotNull Player player);

    boolean isInGame(@NotNull Player player);

    List<String> validateArena(@NotNull Arena arena);

    void joinGame(@NotNull Player player, @NotNull String arenaName);

    void joinGame(@NotNull Player player, @NotNull String arenaName, @Nullable String teamName);

    void leaveGame(@NotNull Player player);

    void startGame(@NotNull String arenaName);

    void removePlayerMapping(@NotNull Player player);

    void removeGame(@NotNull String arenaName);
}
