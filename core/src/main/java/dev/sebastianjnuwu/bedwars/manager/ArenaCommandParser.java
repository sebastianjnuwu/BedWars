package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Normaliza a lista de comandos habilitados de uma arena ({@code enabled-commands}),
 * aceitando string única ou lista e removendo a barra inicial.
 */
final class ArenaCommandParser {

    private ArenaCommandParser() {
    }

    static List<String> parse(final Object raw) {
        final List<String> commands = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (final Object item : list) {
                final String cmd = normalize(item);
                if (cmd != null) {
                    commands.add(cmd);
                }
            }
        } else {
            final String cmd = normalize(raw);
            if (cmd != null) {
                commands.add(cmd);
            }
        }
        return commands;
    }

    private static @Nullable String normalize(final Object raw) {
        if (raw == null) {
            return null;
        }
        String cmd = String.valueOf(raw).trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return null;
        }
        return cmd.toLowerCase();
    }
}