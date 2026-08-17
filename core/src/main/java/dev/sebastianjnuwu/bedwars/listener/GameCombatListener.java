package dev.sebastianjnuwu.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import dev.sebastianjnuwu.bedwars.api.events.GamePlayerDamageByPlayerEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerKillEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStatChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStreakEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

/**
 * Listener responsável pela lógica de combate da partida de BedWars.
 * <p>
 * Gerencia eventos de morte, respawn, dano no vazio, dano geral e dano
 * entre jogadores (friendly fire bloqueado).
 * </p>
 */
public class GameCombatListener implements Listener {

    private final GameManager gameManager;

    public GameCombatListener(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        final Player killer = player.getKiller();
        final GamePlayer victimGP = game.getGamePlayer(player);
        if (killer != null) {
            final GamePlayer killerGP = game.getGamePlayer(killer);
            if (killerGP != null) {
                final int oldKills = killerGP.getKills();
                killerGP.addKill();
                if (victimGP != null) {
                    Bukkit.getPluginManager().callEvent(new GamePlayerKillEvent(game, killerGP, victimGP));
                }
                Bukkit.getPluginManager().callEvent(new GamePlayerStatChangeEvent(game, killerGP, StatType.KILLS, oldKills, killerGP.getKills()));
                final int streak = killerGP.getKills();
                if (streak > 0 && streak % 5 == 0) {
                    Bukkit.getPluginManager().callEvent(new GamePlayerStreakEvent(game, killerGP, streak));
                }
            }
        }

        game.killPlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        final ArenaTeam team = game.getPlayerTeam(player);
        if (team == null) {
            return;
        }

        if (game.isBedless(team)) {
            game.becomeSpectator(player);
            final Location target = team.getSpawn() != null
                    ? LocationUtil.findSafeRespawn(team.getSpawn())
                    : game.getArena().getArenaSpawn();
            if (target != null) {
                event.setRespawnLocation(target);
                final Location reassert = target.clone();
                Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> {
                    if (player.isOnline() && this.gameManager.getPlayerGame(player) == game) {
                        player.teleport(reassert);
                    }
                });
            }
            return;
        }

        if (team.getSpawn() != null) {
            final Location target = LocationUtil.findSafeRespawn(team.getSpawn());
            event.setRespawnLocation(target);
            player.setGameMode(GameMode.SPECTATOR);
            final Location reassert = target.clone();
            Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> {
                if (player.isOnline() && this.gameManager.getPlayerGame(player) == game) {
                    player.teleport(reassert);
                }
            });
        }
    }

    @EventHandler
    public void onVoidDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        event.setCancelled(true);

        if (game.getState() != GameState.PLAYING) {
            final Location spawn = game.getArena().getArenaSpawn();
            player.teleport(spawn != null ? spawn : player.getWorld().getSpawnLocation());
            player.setHealth(20);
            player.setFoodLevel(20);
            return;
        }

        player.setHealth(0);
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        if (game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof final Player victim)) {
            return;
        }
        if (!(event.getDamager() instanceof final Player attacker)) {
            return;
        }

        final Game game = this.gameManager.getPlayerGame(victim);
        if (game == null) {
            return;
        }

        if (game.getState() != GameState.PLAYING) {
            event.setCancelled(true);
            return;
        }

        final ArenaTeam victimTeam = game.getPlayerTeam(victim);
        final ArenaTeam attackerTeam = game.getPlayerTeam(attacker);

        if (victimTeam != null && attackerTeam != null
                && victimTeam.getName().equals(attackerTeam.getName())) {
            event.setCancelled(true);
            return;
        }

        final GamePlayer victimGP = game.getGamePlayer(victim);
        final GamePlayer attackerGP = game.getGamePlayer(attacker);
        if (victimGP != null && attackerGP != null) {
            final GamePlayerDamageByPlayerEvent dmgEvent = new GamePlayerDamageByPlayerEvent(
                    game, attackerGP, victimGP, event.getDamage());
            Bukkit.getPluginManager().callEvent(dmgEvent);
            if (dmgEvent.isCancelled()) {
                event.setCancelled(true);
            } else if (dmgEvent.getDamage() != event.getDamage()) {
                event.setDamage(dmgEvent.getDamage());
            }
        }
    }
}
