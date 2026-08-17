package dev.sebastianjnuwu.bedwars.game.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Gera códigos públicos de partida com 6 caracteres alfanuméricos em maiúsculas.
 */
public final class GameCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private GameCodeGenerator() {
    }

    /**
     * Gera um código aleatório de 6 caracteres alfanuméricos em maiúsculas.
     *
     * @return o código gerado (não nulo)
     */
    public static String generate() {
        final StringBuilder sb = new StringBuilder(6);
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
