package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaMode;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Buscas de partidas dentro do {@link GameManager}: por nome de arena, por
 * código público, por modo aberto e listagem de códigos de partidas abertas.
 */
final class GameLookup {

    private final GameManager manager;

    GameLookup(final GameManager manager) {
        this.manager = manager;
    }

    /**
     * Busca a primeira partida ativa de uma arena pelo nome.
     *
     * @param arenaName nome da arena (não nulo)
     * @return partida encontrada ou {@code null}
     */
    @Nullable Game findFirstByArenaName(final String arenaName) {
        for (final Game game : this.manager.games.values()) {
            if (game.getArena().getName().equalsIgnoreCase(arenaName)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Busca uma partida pelo código público (case-insensitive).
     *
     * @param code código da partida (não nulo)
     * @return a partida correspondente, ou {@code null} se não existir
     */
    @Nullable Game findGameByCode(final String code) {
        for (final Game game : this.manager.games.values()) {
            if (game.getCode().equalsIgnoreCase(code)) {
                return game;
            }
        }
        return null;
    }

    /**
     * Busca uma partida aberta de uma arena, opcionalmente filtrando pelo modo.
     *
     * @param arenaName nome da arena (não nulo)
     * @param mode      modo desejado ou {@code null} para qualquer
     * @return partida aberta encontrada ou {@code null}
     */
    @Nullable Game findOpenGame(final String arenaName, final @Nullable ArenaMode mode) {
        for (final Game game : this.manager.games.values()) {
            final Arena arena = game.getArena();
            if (arena.getName().equalsIgnoreCase(arenaName)
                    && (mode == null || game.getMode() == mode)
                    && (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)
                    && !game.isFull()) {
                return game;
            }
        }
        return null;
    }

    /**
     * Retorna os códigos das partidas abertas (em lobby/início) de uma arena,
     * usado para autocompletar o argumento {@code --code}.
     *
     * @param arenaName nome da arena (não nulo)
     * @return lista de códigos das partidas abertas (nunca nula)
     */
    List<String> listOpenCodes(final String arenaName) {
        final List<String> codes = new ArrayList<>();
        for (final Game game : this.manager.games.values()) {
            final Arena arena = game.getArena();
            if (arena.getName().equalsIgnoreCase(arenaName)
                    && (game.getState() == GameState.WAITING || game.getState() == GameState.STARTING)
                    && !game.isFull()) {
                codes.add(game.getCode());
            }
        }
        return codes;
    }
}