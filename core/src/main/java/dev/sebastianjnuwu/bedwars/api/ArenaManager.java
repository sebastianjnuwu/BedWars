package dev.sebastianjnuwu.bedwars.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

public interface ArenaManager {

    @Nullable Arena get(@NotNull String name);

    @NotNull Collection<Arena> getAll();

    boolean resetArenaMap(@NotNull String arenaName);
}
