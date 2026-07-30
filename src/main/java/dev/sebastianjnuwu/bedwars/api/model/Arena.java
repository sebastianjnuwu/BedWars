package dev.sebastianjnuwu.bedwars.api.model;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

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

    @Nullable String getDifficulty();

    void setDifficulty(@Nullable String difficulty);

    @Nullable String getTime();

    void setTime(@Nullable String time);

    @Nullable String getWeather();

    void setWeather(@Nullable String weather);

    boolean isCycleDay();

    void setCycleDay(boolean cycleDay);

    boolean isCycleWeather();

    void setCycleWeather(boolean cycleWeather);

    boolean isSpawnMobs();

    void setSpawnMobs(boolean spawnMobs);

    boolean isSpawnAnimals();

    void setSpawnAnimals(boolean spawnAnimals);

    @Nullable String getShop();

    void setShop(@Nullable String shop);

    Map<String, GeneratorConfig> getGeneratorConfigs();

    void setGeneratorConfigs(Map<String, GeneratorConfig> configs);

    int getForgeMaxLevel();

    void setForgeMaxLevel(int maxLevel);

    List<ForgeLevel> getForgeLevels();

    void setForgeLevels(List<ForgeLevel> levels);

    /**
     * Retorna as localizações dos NPCs de loja para esta arena.
     *
     * @return lista de localizações dos NPCs
     */
    List<Location> getShopNpcLocations();

    /**
     * Define as localizações dos NPCs de loja para esta arena.
     *
     * @param locations as localizações dos NPCs
     */
    void setShopNpcLocations(List<Location> locations);

    /**
     * Retorna o nome da skin configurada para os NPCs de loja.
     *
     * @return nome da skin ou null
     */
    @Nullable String getShopNpcSkin();

    /**
     * Define a skin para os NPCs de loja nesta arena.
     *
     * @param skin nome da skin ou null para padrão
     */
    void setShopNpcSkin(@Nullable String skin);
}
