package dev.sebastianjnuwu.bedwars.api.model;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
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

    @Nullable String getMapName();

    void setMapName(String mapName);

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

    int getRespawnDelay();

    void setRespawnDelay(int seconds);

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

    int getForgeDefaultLevel();

    void setForgeDefaultLevel(int defaultLevel);

    List<ForgeLevel> getForgeLevels();

    void setForgeLevels(List<ForgeLevel> levels);

    /**
     * Retorna os NPCs de loja configurados para esta arena.
     *
     * @return lista de NPCs de loja
     */
    List<ShopNpc> getShopNpcs();

    /**
     * Define os NPCs de loja para esta arena.
     *
     * @param npcs lista de NPCs de loja
     */
    void setShopNpcs(List<ShopNpc> npcs);

    /**
     * Retorna os comandos permitidos durante a partida nesta arena
     * (config {@code enable-cmd}). Comandos listados (ex.: {@code "g"})
     * não são bloqueados pelo listener de comandos da partida.
     *
     * @return lista de comandos permitidos (nunca nula)
     */
    List<String> getEnabledCommands();

    /**
     * Define os comandos permitidos durante a partida nesta arena.
     *
     * @param commands lista de comandos permitidos (ex.: {@code "g"})
     */
    void setEnabledCommands(List<String> commands);

    /**
     * Retorna os itens dados ao jogador no início da partida e em cada respawn
     * (config {@code spawn_item}).
     *
     * @return lista de materiais de spawn (nunca nula)
     */
    List<Material> getSpawnItems();

    /**
     * Define os itens dados ao jogador no início da partida e em cada respawn.
     *
     * @param spawnItems lista de materiais de spawn
     */
    void setSpawnItems(List<Material> spawnItems);
}
