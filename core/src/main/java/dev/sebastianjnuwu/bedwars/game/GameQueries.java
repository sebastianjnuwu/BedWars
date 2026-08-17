package dev.sebastianjnuwu.bedwars.game;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;

/**
 * Consultas de jogadores e estado da partida usadas pela fachada {@link Game}.
 * <p>
 * Concentra a busca de {@link GamePlayer}, verificação de espectadores/estado,
 * coleções de jogadores, blocos colocados e broadcast de mensagens.
 * </p>
 */
final class GameQueries {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Game game;

    GameQueries(final Game game) {
        this.game = game;
    }

    boolean isSpectator(final Player player) {
        return this.game.spectators.contains(player.getUniqueId());
    }

    boolean isBedless(final ArenaTeam team) {
        return this.game.bedlessTeams.contains(team);
    }

    boolean isEliminated(final ArenaTeam team) {
        return this.game.eliminatedTeams.contains(team);
    }

    @Nullable ArenaTeam getPlayerTeam(final Player player) {
        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        return gp != null ? gp.getTeam() : null;
    }

    @Nullable GamePlayer getGamePlayer(final Player player) {
        return this.game.players.get(player.getUniqueId());
    }

    boolean isPlaying(final Player player) {
        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        return gp != null && gp.isAlive() && this.game.state == GameState.PLAYING;
    }

    void trackPlacedBlock(final Location location) {
        this.game.placedBlocks.add(blockKey(location));
    }

    boolean isPlacedBlock(final Location location) {
        return this.game.placedBlocks.contains(blockKey(location));
    }

    int getPlayerCount() {
        return this.game.players.size();
    }

    boolean isFull() {
        final int capacity = this.game.arena.getTeams().size() * this.game.lifecycle().maxTeamSlots();
        return this.game.players.size() >= capacity;
    }

    Collection<GamePlayer> getGamePlayers() {
        return this.game.players.values();
    }

    Collection<Player> getPlayers() {
        return this.game.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    Collection<Player> getSpectatorPlayers() {
        return this.game.spectators.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    void broadcast(final String message) {
        final Component component = MM.deserialize(message);
        this.game.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .forEach(p -> CompatProvider.chat().sendMessage(p, component));
    }

    private static String blockKey(final Location location) {
        return location.getWorld().getName() + ":" + location.getBlockX()
                + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}