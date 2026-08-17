package dev.sebastianjnuwu.bedwars.game.lifecycle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Backup e restauração do estado do jogador (inventário do mundo normal) ao
 * entrar/sair da partida, além de voltar a mostrar os jogadores de outras
 * partidas.
 */
final class GamePlayerSnapshot {

    private final Game game;

    GamePlayerSnapshot(final Game game) {
        this.game = game;
    }

    /**
     * Salva o estado do jogador (inventário, etc.) para restauração posterior.
     *
     * @param player jogador cujo estado será salvo (não nulo)
     */
    void save(final Player player) {
        final var manager = this.game.gameManager.getPlayerStateManager();
        if (manager.hasSavedState(player)) {
            if (!this.game.players.containsKey(player.getUniqueId()) && !this.game.spectators.contains(player.getUniqueId())) {
                this.game.gameManager.getPlugin().getLogger().warning(this.game.lang.raw("debug.player_orphan_snapshot", player.getUniqueId().toString()));
                manager.restorePlayerState(player);
            }
        }
        manager.savePlayerState(player);
    }

    /**
     * Restaura o estado salvo do jogador e mostra novamente os jogadores de
     * outras partidas.
     *
     * @param player jogador cujo estado será restaurado (não nulo)
     */
    void restore(final Player player) {
        this.game.gameManager.getPlayerStateManager().restorePlayerState(player);
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this.game == this.game.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.showPlayer(this.game.gameManager.getPlugin(), online);
            online.showPlayer(this.game.gameManager.getPlugin(), player);
        }
    }
}
