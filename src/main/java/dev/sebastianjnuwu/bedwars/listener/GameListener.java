package dev.sebastianjnuwu.bedwars.listener;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import dev.sebastianjnuwu.bedwars.api.events.GamePlayerDamageByPlayerEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerKillEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStatChangeEvent;
import dev.sebastianjnuwu.bedwars.api.events.GamePlayerStreakEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.api.model.StatType;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

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
    @EventHandler(priority = EventPriority.HIGHEST)
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
            final var lobby = this.gameManager.getConfigManager().getLobby();
            if (lobby != null) {
                event.setRespawnLocation(lobby);
                player.setGameMode(GameMode.SURVIVAL);
                Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> this.gameManager.leaveGame(player));
            }
            return;
        }

        if (team.getSpawn() != null) {
            event.setRespawnLocation(team.getSpawn());
            player.setGameMode(GameMode.SPECTATOR);
            // Reafirma a posição no tick seguinte: garante que o jogador nasça no
            // spawn do time mesmo se outro plugin sobrescrever a localização do
            // respawn em prioridade mais alta (HIGHEST/MONITOR).
            final Location target = team.getSpawn().clone();
            Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> {
                if (player.isOnline() && this.gameManager.getPlayerGame(player) == game) {
                    player.teleport(target);
                }
            });
        }
    }

    /**
     * Mata instantaneamente jogadores em partida que caem no vazio.
     * <p>
     * Cancela o dano natural do vazio e define a vida para zero, garantindo
     * que o {@link PlayerDeathEvent} seja disparado normalmente para que a
     * lógica de kill/respawn do jogo seja executada.
     * </p>
     *
     * @param event o evento de dano (não nulo)
     */
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

    /**
     * Manipula a quebra de blocos durante a partida.
     * <p>
     * Spectators e jogadores mortos não podem quebrar nenhum bloco. Para jogadores
     * vivos, o único bloco com tratamento especial é a cama: se for a cama do próprio
     * time o evento é cancelado; se for a cama de um time inimigo, a lógica de
     * destruição de cama é acionada via {@link Game#breakBed(ArenaTeam)}.
     * </p>
     *
     * @param event o evento de quebra de bloco (não nulo)
     */
    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }

        // Spectators e jogadores mortos não podem quebrar blocos
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
            if (bedLoc == null) {
                continue;
            }
            if (!this.isSameBlock(bedLoc, footLoc)) {
                continue;
            }

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

    /**
     * Impede que jogadores em partida cliquem com o botão direito em camas.
     * <p>
     * Sem esse bloqueio o jogador tentaria dormir, o que lança uma exceção
     * ou exibe mensagem estranha em mundos sem ciclo de noite/dia.
     * </p>
     *
     * @param event o evento de interação (não nulo)
     */
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

    /**
     * Envia a mensagem e o título de destruição de cama para todos os jogadores da partida.
     * <p>
     * Apenas players que pertencem a algum time recebem a mensagem. O título
     * é exibido somente para os membros do time que perdeu a cama.
     * </p>
     *
     * @param breaker o jogador que destruiu a cama (não nulo)
     * @param game    a partida em andamento (não nula)
     * @param team    o time que teve a cama destruída (não nulo)
     */
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
            p.sendMessage(msg);
            if (pTeam.getName().equals(team.getName())) {
                p.showTitle(title);
            }
        }
    }

    /**
     * Cancela dano entre jogadores do mesmo time (friendly fire).
     *
     * @param event o evento de dano por entidade (não nulo)
     */
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

        // S permite PvP durante a partida (state PLAYING)
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

    /**
     * Bloqueia comandos durante a partida, exceto /bw.
     *
     * @param event o evento de comando (não nulo)
     */
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
        player.sendMessage(this.lang.text(NamedTextColor.RED, "game.commands_blocked"));
    }

    /**
     * Controla o drop de itens durante a partida.
     * <p>
     * Bloqueia o drop de armaduras (protegendo a armadura de time) e de
     * jogadores mortos/espectadores.
     * </p>
     */
    @EventHandler
    public void onPlayerDropItem(final PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        if (isArmorPiece(event.getItemDrop().getItemStack().getType())) {
            event.setCancelled(true);
            return;
        }
        if (!game.isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede a remoção ou troca de armaduras pelo inventário.
     * <p>
     * Cancela cliques no slot de armadura e shift-clicks sobre peças de
     * armadura, mantendo a armadura de time presa no jogador em qualquer
     * inventário aberto.
     * </p>
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null || !game.isPlaying(player)) {
            return;
        }
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
            return;
        }
        final ItemStack current = event.getCurrentItem();
        if (event.isShiftClick() && current != null && isArmorPiece(current.getType())) {
            event.setCancelled(true);
        }
    }

    /**
     * Impede que peças de armadura sejam movidas para o slot de armadura
     * através de arrastar/soltar, em qualquer inventário aberto.
     */
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof final Player player)) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null || !game.isPlaying(player)) {
            return;
        }
        for (final int rawSlot : event.getRawSlots()) {
            if (event.getView().getSlotType(rawSlot) == InventoryType.SlotType.ARMOR) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Verifica se o material é uma peça de armadura.
     *
     * @param material material a verificar
     * @return {@code true} se é capacete, peitoral, calça ou botas
     */
    private static boolean isArmorPiece(final Material material) {
        final String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    /**
     * Impede que spectators (mortos / eliminados) peguem itens do chão.
     * Jogadores vivos podem coletar normalmente.
     */
    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerPickupItem(final org.bukkit.event.player.PlayerPickupItemEvent event) {
        final Player player = event.getPlayer();
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        if (!game.isPlaying(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(final PrepareItemCraftEvent event) {
        final HumanEntity viewer = event.getView().getPlayer();
        if (viewer instanceof final Player player && this.gameManager.isInGame(player)) {
            event.getInventory().setResult(null);
        }
    }

    /**
     * Impede que spectators (mortos / eliminados) coloquem blocos.
     * Jogadores vivos já são tratados pela lógica normal do jogo.
     */
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

    /**
     * Controla explosões de entidades (ex.: TNT) nas partidas.
     * <p>
     * Remove da lista de blocos destruídos todo bloco que não foi colocado
     * pelo jogador, protegendo o mapa original (camas, geradores e plataformas)
     * e mantendo o dano aos jogadores.
     * </p>
     */
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

    /**
     * Manipula a saída do jogador do servidor durante uma partida.
     * <p>
     * Delega ao {@link dev.sebastianjnuwu.bedwars.manager.GameManager#leaveGame(Player)}
     * para limpar o estado do jogador e recalcular a condição de vitória.
     * </p>
     *
     * @param event o evento de saída (não nulo)
     */
    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }
    }

    @EventHandler
    public void onPlayerKick(final PlayerKickEvent event) {
        final Player player = event.getPlayer();
        if (this.gameManager.isInGame(player)) {
            this.gameManager.leaveGame(player);
        }
    }

    /**
     * Verifica se duas localizações referem-se ao mesmo bloco (mesmo mundo e coordenadas inteiras).
     *
     * @param a primeira localização (não nula)
     * @param b segunda localização (não nula)
     * @return {@code true} se ambas apontam para o mesmo bloco
     */
    private boolean isSameBlock(final Location a, final Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}
