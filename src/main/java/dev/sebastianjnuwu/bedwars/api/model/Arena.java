package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Representa uma arena no jogo BedWars.
 * <p>
 * Uma arena contém todas as configurações necessárias para uma partida, incluindo localização do lobby,
 * tamanho do schematic, times, geradores e configurações de contagem regressiva.
 * </p>
 * <p>
 * Esta interface não é thread-safe. As implementações devem gerenciar o acesso concorrente
 * externamente quando necessário.
 * </p>
 * <p>
 * Exemplo de uso:
 * <pre>{@code
 * Arena arena = ...;
 * String nome = arena.getName();
 * arena.setEnabled(true);
 * List<ArenaTeam> times = arena.getTeams();
 * }</pre>
 * </p>
 */
public interface Arena {

    String getName();

    @Nullable Location getLobby();

    void setLobby(Location lobby);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    @Nullable String getWorldName();

    void setWorldName(String worldName);

    int getPasteX();

    int getPasteY();

    int getPasteZ();

    void setPaste(int x, int y, int z);

    int getSchematicWidth();

    int getSchematicHeight();

    int getSchematicLength();

    void setSchematicSize(int width, int height, int length);

    @Nullable Location getArenaSpawn();

    void setArenaSpawn(Location arenaSpawn);

    @Nullable String getSpawnBlockData();

    void setSpawnBlockData(String spawnBlockData);

    List<ArenaTeam> getTeams();

    @Nullable ArenaTeam getTeam(String colorName);

    void addTeam(ArenaTeam team);

    boolean removeTeam(String colorName);

    List<ArenaGenerator> getGenerators();

    void addGenerator(ArenaGenerator generator);

    int getMinPlayers();

    void setMinPlayers(int minPlayers);

    int getCountdown();

    void setCountdown(int countdown);
}
