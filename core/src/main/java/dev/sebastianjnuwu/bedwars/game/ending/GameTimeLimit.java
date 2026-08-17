package dev.sebastianjnuwu.bedwars.game.ending;

import java.time.Duration;
import java.util.UUID;

import org.bukkit.Sound;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.game.ticker.GameTicker;

/**
 * Gerencia o limite de tempo da partida: avisos em marcos, contagem final e
 * escolha do vencedor quando o tempo esgota (por jogadores vivos, berço e kills).
 */
final class GameTimeLimit {

    private final Game game;

    GameTimeLimit(final Game game) {
        this.game = game;
    }

    void handle() {
        final int limitSeconds = this.game.arena.getTimeLimit();
        if (limitSeconds <= 0) {
            return;
        }
        final int remaining = limitSeconds - (this.game.tick / 20);
        if (remaining > 0 && (remaining == 60 || remaining == 45 || remaining == 30 || remaining == 15 || remaining == 10)
                && this.game.timeLimitWarningsSent.add(remaining)) {
            this.game.chat.broadcast(this.game.lang.text(NamedTextColor.GOLD, "game.time_limit_warning_once", remaining));
            this.game.debug("debug.time_limit_warning_sent", this.game.arena.getName(), remaining, limitSeconds);
        }
        if (remaining > 0 && remaining <= 5 && remaining != this.game.timeLimitWarning) {
            this.game.timeLimitWarning = remaining;
            final Title title = Title.title(
                    Component.text("§e" + remaining),
                    this.game.lang.text("game.time_limit_ending"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500)));
            this.game.chat.showTitle(title);
            this.game.chat.playSound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, GameTicker.countdownPitch(remaining, 5));
        }
        if (remaining <= 0) {
            this.forceTimeLimitEnd();
        }
    }

    private void forceTimeLimitEnd() {
        ArenaTeam winner = null;
        for (final var entry : this.game.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.game.eliminatedTeams.contains(team)) {
                continue;
            }
            if (winner == null || this.compareForTimeLimit(team, winner) > 0) {
                winner = team;
            }
        }
        if (winner != null && !this.hasTieForTimeLimit(winner)) {
            this.game.chat.broadcast(this.game.lang.text(NamedTextColor.GOLD, "game.time_limit_winner",
                    winner.getName().toUpperCase()));
            this.game.ending().endGame(winner);
        } else {
            this.game.chat.broadcast(this.game.lang.text(NamedTextColor.RED, "game.time_limit_tie"));
            this.game.ending().forceEnd();
        }
    }

    private int compareForTimeLimit(final ArenaTeam first, final ArenaTeam second) {
        final int alive = Integer.compare(this.game.combat().getAliveCount(first), this.game.combat().getAliveCount(second));
        if (alive != 0) {
            return alive;
        }
        final boolean firstBed = !this.game.bedlessTeams.contains(first);
        final boolean secondBed = !this.game.bedlessTeams.contains(second);
        if (firstBed != secondBed) {
            return firstBed ? 1 : -1;
        }
        return Integer.compare(this.getTeamKills(first), this.getTeamKills(second));
    }

    private boolean hasTieForTimeLimit(final ArenaTeam winner) {
        for (final ArenaTeam team : this.game.teams.keySet()) {
            if (this.game.eliminatedTeams.contains(team) || team.equals(winner)) {
                continue;
            }
            if (this.compareForTimeLimit(team, winner) == 0) {
                return true;
            }
        }
        return false;
    }

    private int getTeamKills(final ArenaTeam team) {
        int total = 0;
        for (final UUID uuid : this.game.teams.get(team)) {
            final GamePlayer gamePlayer = this.game.players.get(uuid);
            if (gamePlayer != null) {
                total += gamePlayer.getKills();
            }
        }
        return total;
    }
}
