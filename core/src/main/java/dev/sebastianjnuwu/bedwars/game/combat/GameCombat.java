package dev.sebastianjnuwu.bedwars.game.combat;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.events.BedBreakEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerDeathEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerEliminateEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStatChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerKillEvent;
import dev.sebastianjnuwu.bedwars.api.events.PlayerRespawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.TeamEliminateEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.DeathCause;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.game.GameItems;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Responsável pela lógica de combate da partida: morte de jogadores,
 * renascimento, quebra de berços, eliminação de times e condição de vitória.
 */
public final class GameCombat {

    private final Game game;

    /**
     * Cria o gerenciador de combate para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameCombat(final Game game) {
        this.game = game;
    }

    /**
     * Mata o jogador informado: marca como morto, dispara os eventos e agenda o
     * renascimento. Se o time não tem mais berço, o jogador é eliminado.
     *
     * @param player jogador que morreu (não nulo)
     */
    public void killPlayer(final Player player) {
        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        if (gp == null || !gp.isAlive()) {
            return;
        }
        final int oldDeaths = gp.getDeaths();
        gp.setAlive(false);
        gp.addDeath();

        final ArenaTeam team = gp.getTeam();
        Bukkit.getPluginManager().callEvent(new PlayerKillEvent(this.game, null, player, null, team));
        Bukkit.getPluginManager().callEvent(new GamePlayerDeathEvent(this.game, gp, DeathCause.CUSTOM));
        Bukkit.getPluginManager().callEvent(new GamePlayerStatChangeEvent(this.game, gp, StatType.DEATHS, oldDeaths, gp.getDeaths()));

        if (this.game.bedlessTeams.contains(team)) {
            Bukkit.getPluginManager().callEvent(new GamePlayerEliminateEvent(this.game, gp, null));
            CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.no_bed"));
            this.game.debug("debug.player_eliminated", player.getName(), this.game.arena.getName(),
                    team.getName());
            Bukkit.getScheduler().runTask(this.game.gameManager.getPlugin(), () -> {
                if (player.isOnline()) {
                    player.spigot().respawn();
                }
            });
            if (this.getAliveCount(team) == 0) {
                this.eliminateTeam(team);
            }
            return;
        }

        this.game.respawnTicks.put(player.getUniqueId(), this.game.arena.getRespawnDelay() * 20);
        CompatProvider.chat().showTitle(player, Title.title(
                this.game.lang.text("game.died_title"),
                this.game.lang.text("game.respawn_subtitle", this.game.arena.getRespawnDelay()),
                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(1), java.time.Duration.ofMillis(500))));
        this.game.debug("debug.player_died", player.getName(), this.game.arena.getName(),
                this.game.arena.getRespawnDelay());
        Bukkit.getScheduler().runTask(this.game.gameManager.getPlugin(), () -> player.spigot().respawn());
        this.game.ticker().startGameTick();
    }

    /**
     * Tenta renascer o jogador no spawn do time. Se o time perdeu o berço e este
     * não é o renascimento final, o jogador vira espectador.
     *
     * @param player jogador a renascer (não nulo)
     * @param team   time do jogador (não nulo)
     */
    public void tryRespawn(final Player player, final ArenaTeam team) {
        final boolean finalRespawn = this.game.pendingFinalRespawns.remove(player.getUniqueId());
        this.game.respawnTicks.remove(player.getUniqueId());
        if (this.game.state != GameState.PLAYING) {
            return;
        }
        if (this.game.players.get(player.getUniqueId()) == null) {
            return;
        }
        if (this.game.bedlessTeams.contains(team) && !finalRespawn) {
            this.game.lifecycle().becomeSpectator(player);
            final Location target = team.getSpawn() != null
                    ? LocationUtil.findSafeRespawn(team.getSpawn())
                    : null;
            if (target != null) {
                LocationUtil.safeTeleport(player, target);
            }
            return;
        }
        if (team.getSpawn() == null) {
            return;
        }

        LocationUtil.safeTeleport(player, LocationUtil.findSafeRespawn(team.getSpawn()));
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.getInventory().clear();
        GameItems.applyTeamArmor(player, team);
        this.game.items().giveSpawnItems(player);
        this.game.upgrades().applyTeamUpgrades(player, team);
        CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.GREEN, "game.respawned"));

        final GamePlayer gp = this.game.players.get(player.getUniqueId());
        if (gp != null) {
            gp.setAlive(true);
        }

        Bukkit.getPluginManager().callEvent(new PlayerRespawnEvent(this.game, player, team));
    }

    /**
     * Quebra o berço do time: marca como sem berço, anuncia para a partida e
     * elimina o time se nenhum jogador estiver vivo.
     *
     * @param team time cujo berço foi quebrado (não nulo)
     */
    public void breakBed(final ArenaTeam team) {
        if (this.game.bedlessTeams.contains(team)) {
            return;
        }
        this.game.bedlessTeams.add(team);

        final Component msg = this.game.lang.text(NamedTextColor.RED, "game.bed_broken", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text(this.game.lang.raw("game.bed_broken_title")),
                Component.text(this.game.lang.raw("game.bed_broken_subtitle", team.getName().toUpperCase())),
                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(3), java.time.Duration.ofSeconds(1))
        );
        this.game.chat.broadcast(msg);
        this.game.chat.playSound(Sound.ENTITY_WITHER_BREAK_BLOCK, 1.0F, 1.0F);
        for (final Player p : this.game.chat.getPresentPlayers()) {
            final ArenaTeam pt = this.game.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(team.getName())) {
                CompatProvider.chat().showTitle(p, title);
            }
        }

        Bukkit.getPluginManager().callEvent(new BedBreakEvent(this.game, team, null));

        for (final UUID uuid : this.game.teams.get(team)) {
            if (this.game.respawnTicks.containsKey(uuid)) {
                this.game.pendingFinalRespawns.add(uuid);
                continue;
            }
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null && this.game.players.containsKey(uuid) && !this.game.players.get(uuid).isAlive()) {
                player.setGameMode(GameMode.SPECTATOR);
                CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.no_bed"));
            }
        }

        if (this.getAliveCount(team) == 0) {
            for (final UUID uuid : this.game.teams.get(team)) {
                this.game.respawnTicks.remove(uuid);
                this.game.pendingFinalRespawns.remove(uuid);
            }
            this.eliminateTeam(team);
        }
    }

    /**
     * Elimina o time da partida, anuncia e verifica a condição de vitória.
     *
     * @param team time eliminado (não nulo)
     */
    public void eliminateTeam(final ArenaTeam team) {
        if (this.game.eliminatedTeams.contains(team)) {
            return;
        }
        this.game.eliminatedTeams.add(team);

        final Component msg = this.game.lang.text(NamedTextColor.GRAY, "game.team_eliminated", team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text(this.game.lang.raw("game.team_eliminated_title")),
                Component.text(this.game.lang.raw("game.team_eliminated_subtitle", team.getName().toUpperCase())),
                Title.Times.times(java.time.Duration.ZERO, java.time.Duration.ofSeconds(3), java.time.Duration.ofSeconds(1))
        );
        this.game.chat.broadcast(msg);
        this.game.chat.playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
        for (final Player p : this.game.chat.getPresentPlayers()) {
            final ArenaTeam pt = this.game.getPlayerTeam(p);
            if (pt != null && pt.getName().equals(team.getName())) {
                CompatProvider.chat().showTitle(p, title);
            }
        }

        Bukkit.getPluginManager().callEvent(new TeamEliminateEvent(this.game, team));

        for (final UUID uuid : this.game.teams.get(team)) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.setGameMode(GameMode.SPECTATOR);
                CompatProvider.chat().sendMessage(player, this.game.lang.text(NamedTextColor.RED, "game.you_eliminated"));
            }
        }

        this.checkWinCondition();
    }

    /**
     * Verifica a condição de vitória: se restar apenas um time com jogadores ou
     * berço, o jogo termina; se nenhum, força o fim.
     */
    public void checkWinCondition() {
        if (this.game.state != GameState.PLAYING) {
            return;
        }
        ArenaTeam winner = null;
        int aliveTeams = 0;
        for (final var entry : this.game.teams.entrySet()) {
            final ArenaTeam team = entry.getKey();
            if (this.game.eliminatedTeams.contains(team)) {
                continue;
            }
            if (this.getAliveCount(team) > 0 || !this.game.bedlessTeams.contains(team)) {
                aliveTeams++;
                if (winner == null) {
                    winner = team;
                } else {
                    return;
                }
            }
        }

        if (winner != null) {
            this.game.ending().endGame(winner);
        } else if (aliveTeams == 0 && this.game.state == GameState.PLAYING) {
            this.game.ending().forceEnd();
        }
    }

    /**
     * Conta quantos jogadores do time estão vivos.
     *
     * @param team time consultado (não nulo)
     * @return número de jogadores vivos
     */
    public int getAliveCount(final ArenaTeam team) {
        int count = 0;
        for (final UUID uuid : this.game.teams.get(team)) {
            final GamePlayer gp = this.game.players.get(uuid);
            if (gp != null && gp.isAlive()) {
                count++;
            }
        }
        return count;
    }
}
