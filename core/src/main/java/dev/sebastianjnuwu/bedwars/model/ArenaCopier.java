package dev.sebastianjnuwu.bedwars.model;

import java.util.ArrayList;
import java.util.HashMap;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Responsável pela cópia profunda de uma {@link Arena}.
 * <p>
 * Cada partida simultânea do mesmo mapa recebe uma cópia própria, com um mundo
 * dedicado, permitindo que uma única arena hospede várias partidas ao mesmo
 * tempo sem compartilhar estado.
 * </p>
 */
final class ArenaCopier {

    private ArenaCopier() {
    }

    /**
     * Cria uma cópia profunda da arena, usada para instâncias de partida.
     *
     * @param source arena original (não nula)
     * @return cópia independente
     */
    static Arena copy(final Arena source) {
        final Arena copy = new Arena(source.getName());
        copy.lobby = source.lobby;
        copy.enabled = source.enabled;
        copy.worldName = source.worldName;
        copy.mapName = source.mapName;
        copy.pasteX = source.pasteX;
        copy.pasteY = source.pasteY;
        copy.pasteZ = source.pasteZ;
        copy.schematicWidth = source.schematicWidth;
        copy.schematicHeight = source.schematicHeight;
        copy.schematicLength = source.schematicLength;
        copy.arenaSpawn = source.arenaSpawn;
        copy.spawnBlockData = source.spawnBlockData;
        copy.minPlayersPerTeam = source.minPlayersPerTeam;
        copy.maxPlayersPerTeam = source.maxPlayersPerTeam;
        copy.minTeamsToStart = source.minTeamsToStart;
        copy.countdown = source.countdown;
        copy.respawnDelay = source.respawnDelay;
        copy.timeLimit = source.timeLimit;
        copy.difficulty = source.difficulty;
        copy.time = source.time;
        copy.weather = source.weather;
        copy.cycleDay = source.cycleDay;
        copy.cycleWeather = source.cycleWeather;
        copy.spawnMobs = source.spawnMobs;
        copy.spawnAnimals = source.spawnAnimals;
        copy.shop = source.shop;
        copy.generatorConfigs = source.generatorConfigs != null
                ? new HashMap<>(source.generatorConfigs)
                : new HashMap<>();
        copy.levelTimes = source.levelTimes != null
                ? new HashMap<>(source.levelTimes)
                : null;
        copy.shopNpcs = source.shopNpcs != null
                ? new ArrayList<>(source.shopNpcs)
                : new ArrayList<>();
        copy.enabledCommands = source.enabledCommands != null
                ? new ArrayList<>(source.enabledCommands)
                : new ArrayList<>();
        copy.spawnItems = source.spawnItems != null
                ? new ArrayList<>(source.spawnItems)
                : new ArrayList<>();
        for (final ArenaGenerator gen : source.generators) {
            final dev.sebastianjnuwu.bedwars.model.ArenaGenerator genCopy =
                    new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(gen.getUniqueId(), gen.getType(), gen.getLocation());
            genCopy.setTeam(gen.getTeam());
            genCopy.setOriginBlockData(gen.getOriginBlockData());
            genCopy.setOriginBlockDataAbove(gen.getOriginBlockDataAbove());
            copy.generators.add(genCopy);
        }
        for (final ArenaTeam team : source.teams) {
            final dev.sebastianjnuwu.bedwars.model.ArenaTeam teamCopy =
                    new dev.sebastianjnuwu.bedwars.model.ArenaTeam(team.getName(), team.getColor());
            teamCopy.setSpawn(team.getSpawn());
            teamCopy.setSpawnBlockData(team.getSpawnBlockData());
            teamCopy.setBed(team.getBed());
            teamCopy.setBedFacing(team.getBedFacing());
            if (team.getForge() != null) {
                for (final ArenaGenerator genCopy : copy.generators) {
                    if (genCopy.getUniqueId().equals(team.getForge().getUniqueId())) {
                        teamCopy.setForge(genCopy);
                        break;
                    }
                }
            }
            copy.teams.add(teamCopy);
        }
        return copy;
    }
}
