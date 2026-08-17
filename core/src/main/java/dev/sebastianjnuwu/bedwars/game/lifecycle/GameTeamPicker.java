package dev.sebastianjnuwu.bedwars.game.lifecycle;

import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Seleção de times dentro da partida: escolha do menor time, busca por nome e
 * cálculo da capacidade máxima de jogadores por time.
 */
final class GameTeamPicker {

    private final Game game;

    GameTeamPicker(final Game game) {
        this.game = game;
    }

    /**
     * Escolhe o time com menos jogadores atualmente.
     *
     * @return o menor time ou {@code null} se não houver times disponíveis
     */
    @Nullable ArenaTeam findSmallestTeam() {
        return this.game.teams.keySet().stream()
                .filter(t -> !this.game.eliminatedTeams.contains(t))
                .min(Comparator.comparingInt(t -> this.game.teams.get(t).size()))
                .orElse(null);
    }

    /**
     * Busca um time pelo nome (ignorando maiúsculas/minúsculas).
     *
     * @param name nome do time
     * @return o time encontrado ou {@code null}
     */
    @Nullable ArenaTeam findNamedTeam(final String name) {
        for (final ArenaTeam team : this.game.teams.keySet()) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    /**
     * Calcula o número máximo de jogadores por time desta partida.
     *
     * @return capacidade máxima por time
     */
    int maxTeamSlots() {
        final int arenaMax = this.game.arena.getMaxPlayersPerTeam();
        if (arenaMax > 0) {
            return arenaMax;
        }
        final ArenaMode mode = this.game.mode;
        if (mode != null) {
            return mode.getTeamSize();
        }
        return largestValidMode().getTeamSize();
    }

    private ArenaMode largestValidMode() {
        final int teamCount = this.game.teams.size();
        ArenaMode largest = ArenaMode.SOLO;
        for (final ArenaMode mode : ArenaMode.values()) {
            if (mode.isValidFor(teamCount) && mode.getTeamSize() > largest.getTeamSize()) {
                largest = mode;
            }
        }
        return largest;
    }
}
