package dev.sebastianjnuwu.bedwars.game.util;

import org.bukkit.Bukkit;

import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Registra mensagens de depuração da partida quando o debug está ativo.
 */
public final class GameDebug {

    private GameDebug() {
    }

    /**
     * Registra uma mensagem de depuração quando o debug está ativo.
     *
     * @param game a partida (não nula)
     * @param key  chave da mensagem no arquivo de língua
     * @param args argumentos de formatação da mensagem
     */
    public static void log(final Game game, final String key, final Object... args) {
        if (game.gameManager.getConfigManager().isDebugEnabled()) {
            Bukkit.getLogger().info("[BedWars] " + game.lang.raw(key, args));
        }
    }
}
