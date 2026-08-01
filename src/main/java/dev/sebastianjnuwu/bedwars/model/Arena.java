package dev.sebastianjnuwu.bedwars.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;

/**
 * Representa uma arena de BedWars. Contém todas as informações de configuração
 * de uma arena, incluindo nome, lobby, spawns, times, geradores, dimensões do
 * schematic e dados de sessão de edição.
 */
public class Arena implements dev.sebastianjnuwu.bedwars.api.model.Arena {

    private final String name;
    private Location lobby;
    private boolean enabled;
    private String worldName;
    private int pasteX;
    private int pasteY;
    private int pasteZ;
    private int schematicWidth;
    private int schematicHeight;
    private int schematicLength;
    private Location arenaSpawn;
    private String spawnBlockData;
    private int minPlayers;
    private int countdown;
    private int respawnDelay;
    private String difficulty;
    private String time;
    private String weather;
    private boolean cycleDay;
    private boolean cycleWeather;
    private boolean spawnMobs;
    private boolean spawnAnimals;
    private String shop;
    private Map<String, GeneratorConfig> generatorConfigs;
    private int forgeMaxLevel;
    private List<ForgeLevel> forgeLevels;
    private List<ShopNpc> shopNpcs;
    private final List<ArenaTeam> teams;
    private final List<ArenaGenerator> generators;

    /**
     * Cria uma nova arena.
     *
     * @param name nome único da arena
     */
    public Arena(final String name) {
        this.name = name;
        this.enabled = false;
        this.generatorConfigs = new HashMap<>();
        this.forgeLevels = new ArrayList<>();
        this.shopNpcs = new ArrayList<>();
        this.teams = new ArrayList<>();
        this.generators = new ArrayList<>();
    }

    /**
     * Retorna o nome da arena.
     *
     * @return nome da arena
     */
    public String getName() {
        return this.name;
    }

    /**
     * Retorna o local do lobby desta arena.
     *
     * @return local do lobby ou null se não definido
     */
    public @Nullable Location getLobby() {
        return this.lobby;
    }

    /**
     * Define o local do lobby desta arena.
     *
     * @param lobby local do lobby
     */
    public void setLobby(final Location lobby) {
        this.lobby = lobby;
    }

    /**
     * Verifica se a arena está habilitada para partidas.
     *
     * @return true se habilitada
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Define se a arena está habilitada.
     *
     * @param enabled true para habilitar
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Retorna o nome do mundo de edição da arena.
     *
     * @return nome do mundo
     */
    public @Nullable String getWorldName() {
        return this.worldName;
    }

    /**
     * Define o nome do mundo de edição.
     *
     * @param worldName nome do mundo
     */
    public void setWorldName(final String worldName) {
        this.worldName = worldName;
    }

    /**
     * Retorna a coordenada X onde o schematic foi colado.
     *
     * @return X da base do paste
     */
    public int getPasteX() {
        return this.pasteX;
    }

    /**
     * Retorna a coordenada Y onde o schematic foi colado.
     *
     * @return Y da base do paste
     */
    public int getPasteY() {
        return this.pasteY;
    }

    /**
     * Retorna a coordenada Z onde o schematic foi colado.
     *
     * @return Z da base do paste
     */
    public int getPasteZ() {
        return this.pasteZ;
    }

    /**
     * Define a posição base onde o schematic foi colado.
     *
     * @param x coordenada X
     * @param y coordenada Y
     * @param z coordenada Z
     */
    public void setPaste(final int x, final int y, final int z) {
        this.pasteX = x;
        this.pasteY = y;
        this.pasteZ = z;
    }

    /**
     * Retorna a largura do schematic (eixo X).
     *
     * @return largura
     */
    public int getSchematicWidth() {
        return this.schematicWidth;
    }

    /**
     * Retorna a altura do schematic (eixo Y).
     *
     * @return altura
     */
    public int getSchematicHeight() {
        return this.schematicHeight;
    }

    /**
     * Retorna o comprimento do schematic (eixo Z).
     *
     * @return comprimento
     */
    public int getSchematicLength() {
        return this.schematicLength;
    }

    /**
     * Define as dimensões do schematic.
     *
     * @param width  largura
     * @param height altura
     * @param length comprimento
     */
    public void setSchematicSize(final int width, final int height, final int length) {
        this.schematicWidth = width;
        this.schematicHeight = height;
        this.schematicLength = length;
    }

    /**
     * Retorna o local de spawn inicial da arena (onde os jogadores aparecem).
     *
     * @return local do spawn da arena ou null
     */
    public @Nullable Location getArenaSpawn() {
        return this.arenaSpawn;
    }

    /**
     * Define o local de spawn inicial da arena.
     *
     * @param arenaSpawn local do spawn
     */
    public void setArenaSpawn(final Location arenaSpawn) {
        this.arenaSpawn = arenaSpawn;
    }

    /**
     * Retorna a lista de times da arena.
     *
     * @return times
     */
    public List<ArenaTeam> getTeams() {
        return this.teams;
    }

    /**
     * Adiciona um time à arena.
     *
     * @param team time
     */
    public void addTeam(final ArenaTeam team) {
        this.teams.add(team);
    }

    /**
     * Retorna um time pelo nome da cor.
     *
     * @param colorName nome da cor
     * @return time ou null
     */
    public @Nullable ArenaTeam getTeam(final String colorName) {
        for (final ArenaTeam team : this.teams) {
            if (team.getName().equalsIgnoreCase(colorName)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Remove um time pelo nome da cor.
     *
     * @param colorName nome da cor
     * @return true se removeu
     */
    public boolean removeTeam(final String colorName) {
        return this.teams.removeIf(t -> t.getName().equalsIgnoreCase(colorName));
    }

    /**
     * Retorna a lista de geradores da arena.
     *
     * @return geradores
     */
    public List<ArenaGenerator> getGenerators() {
        return this.generators;
    }

    /**
     * Adiciona um gerador à arena.
     *
     * @param generator gerador
     */
    public void addGenerator(final ArenaGenerator generator) {
        this.generators.add(generator);
    }

    public int getMinPlayers() {
        return this.minPlayers;
    }

    public void setMinPlayers(final int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getCountdown() {
        return this.countdown;
    }

    public void setCountdown(final int countdown) {
        this.countdown = countdown;
    }

    public int getRespawnDelay() {
        return this.respawnDelay;
    }

    public void setRespawnDelay(final int seconds) {
        this.respawnDelay = seconds;
    }

    public @Nullable String getDifficulty() {
        return this.difficulty;
    }

    public void setDifficulty(final @Nullable String difficulty) {
        this.difficulty = difficulty;
    }

    public @Nullable String getTime() {
        return this.time;
    }

    public void setTime(final @Nullable String time) {
        this.time = time;
    }

    public @Nullable String getWeather() {
        return this.weather;
    }

    public void setWeather(final @Nullable String weather) {
        this.weather = weather;
    }

    public boolean isCycleDay() {
        return this.cycleDay;
    }

    public void setCycleDay(final boolean cycleDay) {
        this.cycleDay = cycleDay;
    }

    public boolean isCycleWeather() {
        return this.cycleWeather;
    }

    public void setCycleWeather(final boolean cycleWeather) {
        this.cycleWeather = cycleWeather;
    }

    public boolean isSpawnMobs() {
        return this.spawnMobs;
    }

    public void setSpawnMobs(final boolean spawnMobs) {
        this.spawnMobs = spawnMobs;
    }

    public boolean isSpawnAnimals() {
        return this.spawnAnimals;
    }

    public void setSpawnAnimals(final boolean spawnAnimals) {
        this.spawnAnimals = spawnAnimals;
    }

    /**
     * Retorna o BlockData original do bloco abaixo do spawn (antes de virar esmeralda).
     *
     * @return string do BlockData original
     */
    public @Nullable String getSpawnBlockData() {
        return this.spawnBlockData;
    }

    /**
     * Define o BlockData original do bloco abaixo do spawn.
     *
     * @param spawnBlockData string do BlockData original
     */
    public void setSpawnBlockData(final String spawnBlockData) {
        this.spawnBlockData = spawnBlockData;
    }

    public @Nullable String getShop() {
        return shop;
    }

    public void setShop(@Nullable String shop) {
        this.shop = shop;
    }

    public Map<String, GeneratorConfig> getGeneratorConfigs() {
        return generatorConfigs;
    }

    public void setGeneratorConfigs(Map<String, GeneratorConfig> configs) {
        this.generatorConfigs = configs;
    }

    public int getForgeMaxLevel() {
        return forgeMaxLevel;
    }

    public void setForgeMaxLevel(int maxLevel) {
        this.forgeMaxLevel = maxLevel;
    }

    public List<ForgeLevel> getForgeLevels() {
        return forgeLevels;
    }

    public void setForgeLevels(List<ForgeLevel> levels) {
        this.forgeLevels = levels;
    }

    /**
     * Retorna os NPCs da loja desta arena.
     *
     * @return lista de NPCs da loja
     */
    public List<ShopNpc> getShopNpcs() {
        return shopNpcs;
    }

    /**
     * Define os NPCs da loja desta arena.
     *
     * @param npcs lista de NPCs da loja
     */
    public void setShopNpcs(List<ShopNpc> npcs) {
        this.shopNpcs = npcs;
    }

    /**
     * Restaura o bloco original abaixo do spawn (remove a esmeralda).
     */
    public void restoreSpawnBlock() {
        if (this.arenaSpawn == null || this.spawnBlockData == null) {
            return;
        }
        final var block = this.arenaSpawn.getBlock().getRelative(0, -1, 0);
        block.setBlockData(org.bukkit.Bukkit.createBlockData(this.spawnBlockData), false);
    }

    /**
     * Limpa os dados de sessão de edição.
     */
    public void clearSession() {
        this.worldName = null;
    }
}
