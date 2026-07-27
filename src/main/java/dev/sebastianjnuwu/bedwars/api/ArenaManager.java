package dev.sebastianjnuwu.bedwars.api;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface ArenaManager {

    @Nullable Arena get(@NotNull String name);

    @NotNull Collection<Arena> getAll();

    boolean resetArenaMap(@NotNull String arenaName);
}
