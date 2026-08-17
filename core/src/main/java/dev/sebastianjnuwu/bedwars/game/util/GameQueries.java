package dev.sebastianjnuwu.bedwars.game.util;

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
import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Consultas de jogadores e estado da partida usadas pela fachada {@link Game}.
 * <p>
 * Concentra a busca de {@link GamePlayer}, verificação de espectadores/estado,
 * coleções de jogadores, blocos colocados e broadcast de mensagens.
 * </p>
 */
public final class GameQueries {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Game game;

    public GameQueries(final Game game) {
        this.game = game;
    }

    public boolean isSpectator(final Player player) {
        return this.game.spectators.contains(player.getUniqueId());
    }

    public boolean isBedless(final ArenaTeam team) {
        return this.game.bedlessTeams.contains(team);
    }

    public boolean isEliminated(final ArenaTeam team) {
        return this.game.eliminatedTeams.contains(team);
    }

    @Nullable
    public ArenaTeam getPlayerTeam(final Player player) {
        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        return gp != null ? gp.getTeam() : null;
    }

    @Nullable
    public GamePlayer getGamePlayer(final Player player) {
        return this.game.players.get(player.getUniqueId());
    }

    public boolean isPlaying(final Player player) {
        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        return gp != null && gp.isAlive() && this.game.state == GameState.PLAYING;
    }

    public void trackPlacedBlock(final Location location) {
        this.game.placedBlocks.add(blockKey(location));
    }

    public boolean isPlacedBlock(final Location location) {
        return this.game.placedBlocks.contains(blockKey(location));
    }

    public int getPlayerCount() {
        return this.game.players.size();
    }

    public boolean isFull() {
        final int capacity = this.game.arena.getTeams().size() * this.game.lifecycle().maxTeamSlots();
        return this.game.players.size() >= capacity;
    }

    public Collection<GamePlayer> getGamePlayers() {
        return this.game.players.values();
    }

    public Collection<Player> getPlayers() {
        return this.game.players.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<Player> getSpectatorPlayers() {
        return this.game.spectators.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void broadcast(final String message) {
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
