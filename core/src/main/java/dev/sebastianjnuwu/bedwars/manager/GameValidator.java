package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.List;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Valida a configuração de uma arena para partida: spawn, times, camas e
 * forjas por time. Retorna a lista de pendências em português.
 */
final class GameValidator {

    private final GameManager manager;

    GameValidator(final GameManager manager) {
        this.manager = manager;
    }

    /**
     * Valida a configuração da arena para uma partida.
     *
     * @param arena arena a validar (não nula)
     * @return lista de pendências de configuração (nunca nula)
     */
    List<String> validate(final Arena arena) {
        final List<String> missing = new ArrayList<>();
        if (arena.getArenaSpawn() == null) {
            missing.add(this.manager.lang.raw("game.validate_spawn", arena.getName()));
        }
        if (arena.getTeams().size() < 2) {
            missing.add(this.manager.lang.raw("game.validate_teams", arena.getName()));
        }
        if (arena.getMinTeamsToStart() > arena.getTeams().size()) {
            missing.add(this.manager.lang.raw("game.validate_min_teams", arena.getName()));
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() == null) {
                missing.add(this.manager.lang.raw("game.validate_team_spawn", team.getName()));
            }
            if (team.getBed() == null) {
                missing.add(this.manager.lang.raw("game.validate_team_bed", team.getName()));
            }
            final long forgeCount = arena.getGenerators().stream()
                    .filter(generator -> generator.getType().equalsIgnoreCase("forge"))
                    .filter(generator -> team.getName().equalsIgnoreCase(generator.getTeam()))
                    .count();
            if (forgeCount == 0) {
                missing.add(this.manager.lang.raw("game.validate_team_forge", team.getName()));
            } else if (forgeCount > 1) {
                missing.add(this.manager.lang.raw("game.validate_team_forge_duplicate", team.getName()));
            }
        }
        return missing;
    }
}