package dev.sebastianjnuwu.bedwars.slime;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Aplica as configurações de uma arena (dificuldade, tempo, clima e regras)
 * a um mundo de partida Slime.
 */
final class SlimeWorldSettings {

    private SlimeWorldSettings() {
    }

    /**
     * Aplica as configurações da arena ao mundo.
     *
     * @param world mundo
     * @param arena arena
     */
    @SuppressWarnings("deprecation")
    static void apply(@NotNull World world, @NotNull Arena arena) {
        if (arena.getDifficulty() != null) {
            try {
                world.setDifficulty(org.bukkit.Difficulty.valueOf(arena.getDifficulty().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (arena.getTime() != null) {
            switch (arena.getTime().toUpperCase()) {
                case "DAY" -> world.setTime(1000);
                case "NOON" -> world.setTime(6000);
                case "SUNSET" -> world.setTime(12000);
                case "NIGHT" -> world.setTime(13000);
                case "MIDNIGHT" -> world.setTime(18000);
                default -> {
                }
            }
        }

        if (arena.getWeather() != null) {
            switch (arena.getWeather().toUpperCase()) {
                case "CLEAR" -> {
                    world.setStorm(false);
                    world.setThundering(false);
                }
                case "RAIN" -> {
                    world.setStorm(true);
                    world.setThundering(false);
                }
                case "THUNDER" -> {
                    world.setStorm(true);
                    world.setThundering(true);
                }
                default -> {
                }
            }
        }

        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, arena.isCycleDay());
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, arena.isCycleWeather());
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, arena.isSpawnMobs());
        world.setAnimalSpawnLimit(arena.isSpawnAnimals() ? -1 : 0);
        world.setMonsterSpawnLimit(arena.isSpawnMobs() ? -1 : 0);
    }
}