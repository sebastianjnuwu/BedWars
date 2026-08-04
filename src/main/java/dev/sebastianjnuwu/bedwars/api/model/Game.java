package dev.sebastianjnuwu.bedwars.api.model;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface Game {

    Arena getArena();

    GameState getState();

    boolean isBedless(ArenaTeam team);

    boolean isEliminated(ArenaTeam team);

    @Nullable ArenaTeam getPlayerTeam(Player player);

    @Nullable GamePlayer getGamePlayer(Player player);

    boolean isPlaying(Player player);

    void trackPlacedBlock(org.bukkit.Location location);

    boolean isPlacedBlock(org.bukkit.Location location);

    int getPlayerCount();

    Collection<GamePlayer> getGamePlayers();

    void start();

    void forceEnd();

    Collection<Player> getPlayers();

    void broadcast(String message);

    void join(Player player, @Nullable String teamName);

    void joinAsSpectator(Player player);

    void leave(Player player);

    boolean isSpectator(Player player);

    void breakBed(ArenaTeam team);

    void killPlayer(Player player);
}
