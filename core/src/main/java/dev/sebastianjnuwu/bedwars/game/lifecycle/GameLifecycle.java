package dev.sebastianjnuwu.bedwars.game.lifecycle;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import dev.sebastianjnuwu.bedwars.api.events.PlayerJoinGameEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerLeaveGameEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.game.GameItems;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Responsável pelo ciclo de vida dos jogadores dentro da partida: entrada,
 * troca de time, saída e espectadores. Também cuida do backup/restauração do
 * inventário do mundo normal e da escolha automática de times.
 */
public final class GameLifecycle {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Game game;
    private final GameTeamPicker teamPicker;
    private final GamePlayerSnapshot snapshot;

    /**
     * Cria o gerenciador de ciclo de vida para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameLifecycle(final Game game) {
        this.game = game;
        this.teamPicker = new GameTeamPicker(game);
        this.snapshot = new GamePlayerSnapshot(game);
    }

    /**
     * Adiciona um jogador à partida no menor time disponível.
     *
     * @param player jogador que entrou (não nulo)
     */
    public void join(final Player player) {
        this.join(player, null, true);
    }

    /**
     * Adiciona um jogador à partida no time informado.
     *
     * @param player   jogador que entrou (não nulo)
     * @param teamName nome do time desejado ou {@code null} para escolha automática
     */
    public void join(final Player player, final @Nullable String teamName) {
        this.join(player, teamName, true);
    }

    /**
     * Adiciona um jogador à partida no time informado, com ou sem teleporte.
     *
     * @param player   jogador que entrou (não nulo)
     * @param teamName nome do time desejado ou {@code null} para escolha automática
     * @param teleport se {@code true}, teleporta o jogador para o spawn da arena
     */
    public void join(final Player player, final @Nullable String teamName, final boolean teleport) {
        if (this.game.state != GameState.WAITING && this.game.state != GameState.STARTING) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.in_progress"));
            return;
        }
        if (this.game.players.containsKey(player.getUniqueId())) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }

        final ArenaTeam team;
        if (teamName != null) {
            team = this.teamPicker.findNamedTeam(teamName);
            if (team == null) {
                CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.team_not_found", teamName));
                return;
            }
            if (this.game.teams.get(team).size() >= this.teamPicker.maxTeamSlots()) {
                CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.team_full"));
                return;
            }
        } else {
            team = this.teamPicker.findSmallestTeam();
            if (team == null) {
                CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.no_teams_available"));
                return;
            }
        }

        final var gp = new dev.sebastianjnuwu.bedwars.model.GamePlayer(player.getUniqueId(), team);
        this.game.players.put(player.getUniqueId(), gp);
        this.game.teams.get(team).add(player.getUniqueId());

        this.game.debug("debug.player_joined", player.getName(), this.game.arena.getName(),
                team.getName(), this.game.players.size());

        this.snapshot.save(player);

        Bukkit.getPluginManager().callEvent(new PlayerJoinGameEvent(this.game, player));

        CompatProvider.chat().sendMessage(player, MM.deserialize(this.game.lang.raw("game.game_code", this.game.code)));

        if (teleport) {
            final Location spawn = this.game.arena.getArenaSpawn();
            if (spawn != null) {
                LocationUtil.safeTeleport(player, spawn);
            }
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.ADVENTURE);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().setItem(8, this.game.items().createExitDoorItem());
        player.getInventory().setItem(0, this.game.items().createTeamSelectorItem(team));
        GameItems.applyTeamArmor(player, team);

        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (online == player) {
                continue;
            }
            if (this.game == this.game.gameManager.getPlayerGame(online)) {
                player.showPlayer(this.game.gameManager.getPlugin(), online);
                online.showPlayer(this.game.gameManager.getPlugin(), player);
                continue;
            }
            player.hidePlayer(this.game.gameManager.getPlugin(), online);
            online.hidePlayer(this.game.gameManager.getPlugin(), player);
        }

        final int count = this.game.players.size();
        final int max = this.game.arena.getTeams().size();
        final Component msg = this.game.lang.text(NamedTextColor.GREEN, "game.join_broadcast",
                player.getName(), String.valueOf(count), String.valueOf(max));
        this.game.chat.sendToPlayers(msg);

        this.game.ticker().updateCountdownState();
    }

    /**
     * Troca o jogador de time dentro da partida.
     *
     * @param player   jogador que trocou (não nulo)
     * @param teamName nome do time de destino (não nulo)
     */
    public void switchTeam(final Player player, final String teamName) {
        if (this.game.state != GameState.WAITING && this.game.state != GameState.STARTING) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.in_progress"));
            return;
        }
        final var gp = this.game.players.get(player.getUniqueId());
        if (gp == null) {
            this.join(player, teamName, false);
            return;
        }
        final ArenaTeam oldTeam = gp.getTeam();
        if (oldTeam.getName().equalsIgnoreCase(teamName)) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.already_in_team"));
            return;
        }
        final ArenaTeam newTeam = this.teamPicker.findNamedTeam(teamName);
        if (newTeam == null) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.team_not_found", teamName));
            return;
        }
        if (this.game.teams.get(newTeam).size() >= this.teamPicker.maxTeamSlots()) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.team_full"));
            return;
        }
        this.game.teams.get(oldTeam).remove(player.getUniqueId());
        this.game.teams.get(newTeam).add(player.getUniqueId());
        gp.setTeam(newTeam);
        GameItems.applyTeamArmor(player, newTeam);
        player.getInventory().setItem(0, this.game.items().createTeamSelectorItem(newTeam));
        CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.GREEN, "game.switched_team", newTeam.getName()));
        this.game.ticker().updateCountdownState();
    }

    /**
     * Remove um jogador da partida, restaurando o inventário e voltando para o
     * lobby. Se o time ficar sem jogadores vivos e sem berço, o time é eliminado.
     *
     * @param player jogador que saiu (não nulo)
     */
    public void leave(final Player player) {
        if (this.game.state == GameState.ENDING) {
            this.snapshot.restore(player);
            this.game.gameManager.removePlayerMapping(player);
            return;
        }

        final boolean wasSpectator = this.game.spectators.remove(player.getUniqueId());
        this.game.respawnTicks.remove(player.getUniqueId());
        this.game.pendingFinalRespawns.remove(player.getUniqueId());

        final var gp = this.game.players.remove(player.getUniqueId());
        final ArenaTeam team = gp != null ? gp.getTeam() : null;
        if (team != null) {
            this.game.teams.get(team).remove(player.getUniqueId());
        }

        // Restaura inventario do mundo normal
        this.snapshot.restore(player);

        final Location lobby = this.game.gameManager.getConfigManager().getLobby();
        if (lobby != null) {
            player.teleport(lobby);
        } else if (!Bukkit.getWorlds().isEmpty()) {
            player.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
        player.setGameMode(GameMode.SURVIVAL);

        if (wasSpectator && gp == null) {
            return;
        }

        this.game.debug("debug.player_left", player.getName(), this.game.arena.getName(),
                this.game.players.size());

        // Mostra jogadores de outras partidas novamente
        for (final Player online : Bukkit.getOnlinePlayers()) {
            if (this.game == this.game.gameManager.getPlayerGame(online)) {
                continue;
            }
            player.showPlayer(this.game.gameManager.getPlugin(), online);
            online.showPlayer(this.game.gameManager.getPlugin(), player);
        }

        final Component msg = this.game.lang.text(NamedTextColor.YELLOW, "game.leave_broadcast", player.getName());
        // Envia mensagem apenas para jogadores desta partida
        this.game.chat.sendToPlayers(msg);

        Bukkit.getPluginManager().callEvent(new PlayerLeaveGameEvent(this.game, player));

        this.game.ticker().updateCountdownState();

        if (team != null) {
            if (this.game.bedlessTeams.contains(team) && this.game.combat().getAliveCount(team) == 0) {
                this.game.combat().eliminateTeam(team);
            } else {
                this.game.combat().checkWinCondition();
            }
        }
    }

    /**
     * Adiciona um jogador à partida como espectador.
     *
     * @param player jogador que virou espectador (não nulo)
     */
    public void joinAsSpectator(final Player player) {
        if (this.game.players.containsKey(player.getUniqueId())) {
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.already_in_this_game"));
            return;
        }
        if (this.game.spectators.contains(player.getUniqueId())) {
            return;
        }

        this.game.debug("debug.player_spectator", player.getName(), this.game.arena.getName());
        this.game.spectators.add(player.getUniqueId());
        this.snapshot.save(player);

        CompatProvider.chat().sendMessage(player, MM.deserialize(this.game.lang.raw("game.game_code", this.game.code)));

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().setItem(8, this.game.items().createExitDoorItem());

        final Location spawn = this.game.arena.getArenaSpawn();
        if (spawn != null) {
            LocationUtil.safeTeleport(player, spawn);
        }
    }

    /**
     * Transforma um jogador em espectador (usado quando o time fica sem berço).
     *
     * @param player jogador que virou espectador (não nulo)
     */
    public void becomeSpectator(final Player player) {
        this.game.spectators.add(player.getUniqueId());
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(8, this.game.items().createExitDoorItem());
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    /**
     * Escolhe o time com menos jogadores atualmente.
     *
     * @return o menor time ou {@code null} se não houver times disponíveis
     */
    public @Nullable ArenaTeam findSmallestTeam() {
        return this.teamPicker.findSmallestTeam();
    }

    /**
     * Busca um time pelo nome (ignorando maiúsculas/minúsculas).
     *
     * @param name nome do time
     * @return o time encontrado ou {@code null}
     */
    public @Nullable ArenaTeam findNamedTeam(final String name) {
        return this.teamPicker.findNamedTeam(name);
    }

    /**
     * Calcula o número máximo de jogadores por time desta partida.
     *
     * @return capacidade máxima por time
     */
    public int maxTeamSlots() {
        return this.teamPicker.maxTeamSlots();
    }

    /**
     * Salva o estado do jogador (inventário, etc.) para restauração posterior.
     *
     * @param player jogador cujo estado será salvo (não nulo)
     */
    public void saveInventory(final Player player) {
        this.snapshot.save(player);
    }

    /**
     * Restaura o estado salvo do jogador e mostra novamente os jogadores de
     * outras partidas.
     *
     * @param player jogador cujo estado será restaurado (não nulo)
     */
    public void restoreInventory(final Player player) {
        this.snapshot.restore(player);
    }
}
