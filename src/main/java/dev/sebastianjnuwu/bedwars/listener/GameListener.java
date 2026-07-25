package dev.sebastianjnuwu.bedwars.listener;

import dev.sebastianjnuwu.bedwars.game.Game;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;

/**
 * Listener responsável pela lógica principal da partida de BedWars.
 * <p>
 * Gerencia eventos de morte, respawn, dano no vazio, quebra de camas,
 * interação com camas e saída de jogadores durante uma partida ativa.
 * </p>
 *
 * @see Game
 * @see GameManager
 */
public class GameListener implements Listener {

    private final GameManager gameManager;
    private final LangManager lang;

    /**
     * Constrói um novo {@code GameListener}.
     *
     * @param gameManager gerenciador de partidas (não nulo)
     */
    public GameListener(final GameManager gameManager) {
        this.gameManager = gameManager;
        this.lang = gameManager.getLang();
    }

    /**
     * Manipula o evento de morte de um jogador durante a partida.
     * <p>
     * Remove a mensagem de morte e os drops padrão. Se o jogador foi morto por
     * outro jogador, incrementa o contador de kills do assassino. Em seguida,
     * delega a lógica de eliminação ao jogo.
     * </p>
     *
     * @param event o evento de morte do jogador (não nulo)
     */
    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) return;

        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);

        final Player killer = player.getKiller();
        if (killer != null) {
            final GamePlayer gp = game.getGamePlayer(killer);
            if (gp != null) gp.addKill();
        }

        game.killPlayer(player);
    }

    /**
     * Manipula o evento de renascimento de um jogador.
     * <p>
     * Se o time do jogador estiver sem cama, o respawn é definido para o spawn
     * do mundo e o jogador entra no modo espectador. Caso contrário, o jogador
     * renasce no spawn do time no modo sobrevivência.
     * </p>
     *
     * @param event o evento de renascimento (não nulo)
     */
    @EventHandler
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) return;

        final ArenaTeam team = game.getPlayerTeam(player);
        if (team == null) return;

        if (game.isBedless(team)) {
            event.setRespawnLocation(player.getWorld().getSpawnLocation());
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "game.no_bed"));
            return;
        }

        if (team.getSpawn() != null) {
            event.setRespawnLocation(team.getSpawn());
            player.setGameMode(GameMode.SURVIVAL);
        }
    }

    @EventHandler
    public void onVoidDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.VOID) return;
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) return;

        event.setCancelled(true);
        player.setHealth(0);
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) return;

        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof final Bed bedData)) return;

        // Normalise to the foot block so we always compare against team.getBed()
        // The HEAD part has its type=HEAD; we need to find the foot location.
        final Location clickedLoc = block.getLocation();
        final Location footLoc;
        if (bedData.getPart() == Bed.Part.HEAD) {
            // The foot is in the opposite direction of the facing
            final org.bukkit.block.BlockFace facing = bedData.getFacing();
            footLoc = clickedLoc.clone().add(
                    -facing.getModX(), -facing.getModY(), -facing.getModZ());
        } else {
            footLoc = clickedLoc;
        }

        for (final ArenaTeam team : game.getArena().getTeams()) {
            final Location bedLoc = team.getBed();
            if (bedLoc == null) continue;
            if (!this.isSameBlock(bedLoc, footLoc)) continue;

            // Foot matched — check if the breaker is on this team
            final ArenaTeam playerTeam = game.getPlayerTeam(player);
            if (playerTeam != null && playerTeam.getName().equals(team.getName())) {
                event.setCancelled(true);
                player.sendMessage(this.lang.text(NamedTextColor.RED, "game.cant_break_own_bed"));
                return;
            }

            event.setDropItems(false);
            game.breakBed(team);
            this.broadcastBedBreak(player, game, team);
            return;
        }
    }

    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Bed)) return;
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) return;
        event.setCancelled(true);
    }

    private void broadcastBedBreak(final Player breaker, final Game game, final ArenaTeam team) {
        final Component msg = this.lang.text(NamedTextColor.RED, "game.bed_destroyed", breaker.getName(), team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text("§cCAMA DESTRUÍDA!"),
                Component.text("§e" + team.getName().toUpperCase() + " §7perdeu a cama!"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        for (final Player p : Bukkit.getOnlinePlayers()) {
            final ArenaTeam pTeam = game.getPlayerTeam(p);
            if (pTeam == null) continue;
            p.sendMessage(msg);
            if (pTeam.getName().equals(team.getName())) {
                p.showTitle(title);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }
    }

    private boolean isSameBlock(final Location a, final Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
