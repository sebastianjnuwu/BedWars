package dev.sebastianjnuwu.bedwars.api.model;

import org.jetbrains.annotations.Nullable;

/**
 * Modos de partida de uma arena, definindo quantos jogadores por time.
 */
public enum ArenaMode {

    SOLO(1),
    DOUBLES(2),
    THREES(3),
    FOURS(4);

    private final int teamSize;

    ArenaMode(final int teamSize) {
        this.teamSize = teamSize;
    }

    /**
     * Retorna a quantidade de jogadores por time.
     *
     * @return tamanho do time
     */
    public int getTeamSize() {
        return this.teamSize;
    }

    /**
     * Verifica se o modo é compatível com uma arena com a quantidade de
     * times informada. Um modo é válido quando o número de times é
     * divisível pelo tamanho do time (ex.: 2 times aceitam solo e dupla,
     * mas não trio).
     *
     * @param teamCount quantidade de times do mapa
     * @return {@code true} se o modo pode ser usado
     */
    public boolean isValidFor(final int teamCount) {
        return teamCount % this.teamSize == 0;
    }

    /**
     * Converte um texto (português ou inglês) em um {@link ArenaMode}.
     * Aceita também o prefixo {@code --}.
     *
     * @param input texto a ser interpretado
     * @return o modo correspondente, ou {@code null} se inválido
     */
    public static @Nullable ArenaMode fromAlias(final @Nullable String input) {
        if (input == null) {
            return null;
        }
        final String normalized = input.toLowerCase().replace("--", "").replace("-", "");
        return switch (normalized) {
            case "solo", "single", "s" -> SOLO;
            case "dupla", "duo", "doubles", "double", "d" -> DOUBLES;
            case "trio", "trios", "threes", "three", "t" -> THREES;
            case "quarteto", "quartet", "fours", "four", "quad", "squads", "q" -> FOURS;
            default -> null;
        };
    }
}
