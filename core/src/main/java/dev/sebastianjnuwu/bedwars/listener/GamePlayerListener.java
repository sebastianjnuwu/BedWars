package dev.sebastianjnuwu.bedwars.listener;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

/**
 * Listener responsável por blocos, explosões, comandos e saída de jogadores
 * durante uma partida de BedWars.
 * <p>
 * Gerencia a quebra de camas, o bloqueio de dormir, a colocação de blocos, a
 * proteção do mapa contra explosões, o bloqueio de comandos e a saída/kick de
 * jogadores em partida ativa.
 * </p>
 */
public class GamePlayerListener implements Listener {

    private final GameManager gameManager;
    private final LangManager lang;

    public GamePlayerListener(final GameManager gameManager) {
        this.gameManager = gameManager;
        this.lang = gameManager.getLang();
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        if (!game.isPlaying(player)) {
            event.setCancelled(true);
            return;
        }

        final Block block = event.getBlock();
        if (!(block.getBlockData() instanceof final Bed bedData)) {
            if (!game.isPlacedBlock(block.getLocation())) {
                event.setCancelled(true);
            }
            return;
        }

        final Location clickedLoc = block.getLocation();
        final Location footLoc;
        if (bedData.getPart() == Bed.Part.HEAD) {
            final org.bukkit.block.BlockFace facing = bedData.getFacing();
            footLoc = clickedLoc.clone().add(
                    -facing.getModX(), -facing.getModY(), -facing.getModZ());
        } else {
            footLoc = clickedLoc;
        }

        for (final ArenaTeam team : game.getArena().getTeams()) {
            final Location bedLoc = team.getBed();
            if (bedLoc == null) {
                continue;
            }
            if (!this.isSameBlock(bedLoc, footLoc)) {
                continue;
            }

            final ArenaTeam playerTeam = game.getPlayerTeam(player);
            if (playerTeam != null && playerTeam.getName().equals(team.getName())) {
                event.setCancelled(true);
                CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.cant_break_own_bed"));
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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        final Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Bed)) {
            return;
        }
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        if (!game.isPlaying(player)) {
            event.setCancelled(true);
            return;
        }
        game.trackPlacedBlock(event.getBlockPlaced().getLocation());
    }

    @EventHandler
    public void onEntityExplode(final EntityExplodeEvent event) {
        if (!event.getEntity().getWorld().getName().startsWith("bw_")) {
            return;
        }
        final Game game = this.gameManager.getGameByWorld(event.getEntity().getWorld().getName());
        if (game == null) {
            event.blockList().clear();
            return;
        }
        event.blockList().removeIf(block -> !game.isPlacedBlock(block.getLocation()));
    }

    @EventHandler
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        final String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/bw") || cmd.startsWith("/bedwars")) {
            return;
        }

        for (final String allowed : game.getArena().getEnabledCommands()) {
            final String base = "/" + allowed.toLowerCase();
            if (cmd.equals(base) || cmd.startsWith(base + " ")) {
                return;
            }
        }

        event.setCancelled(true);
        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "game.commands_blocked"));
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        this.gameManager.removeFromPendingJoins(player);
        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        final Player player = event.getPlayer();
        this.gameManager.removeFromPendingJoins(player);
        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }
    }

    private void broadcastBedBreak(final Player breaker, final Game game, final ArenaTeam team) {
        final Component msg = this.lang.text(NamedTextColor.RED, "game.bed_destroyed", breaker.getName(), team.getName().toUpperCase());
        final Title title = Title.title(
                Component.text(this.lang.raw("game.bed_destroyed_title")),
                Component.text(this.lang.raw("game.bed_destroyed_subtitle", team.getName().toUpperCase())),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        for (final Player p : Bukkit.getOnlinePlayers()) {
            final ArenaTeam pTeam = game.getPlayerTeam(p);
            if (pTeam == null) {
                continue;
            }
            CompatProvider.chat().sendMessage(p, msg);
            if (pTeam.getName().equals(team.getName())) {
                CompatProvider.chat().showTitle(p, title);
            }
        }
    }

    private boolean isSameBlock(final Location a, final Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}