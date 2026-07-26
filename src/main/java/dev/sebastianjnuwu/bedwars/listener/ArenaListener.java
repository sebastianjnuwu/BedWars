package dev.sebastianjnuwu.bedwars.listener;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Listener responsável por proteger arenas durante o modo edição e fora de partidas.
 * <p>
 * Garante que jogadores que não estão em uma partida ativa não possam interagir com o mundo
 * da arena (quebrar blocos, colocar blocos, sofrer dano, perder fome ou executar comandos).
 * Durante o modo edição ({@link EditorManager}), jogadores podem modificar livremente a arena.
 * </p>
 *
 * @see EditorManager
 * @see ArenaManager
 */
public class ArenaListener implements Listener {

    private final ArenaManager arenaManager;
    private final GameManager gameManager;
    private final EditorManager editorManager;
    private final LangManager lang;

    /**
     * Constrói um novo {@code ArenaListener}.
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param gameManager   gerenciador de partidas (não nulo)
     * @param editorManager gerenciador do modo edição (não nulo)
     */
    public ArenaListener(final ArenaManager arenaManager, final GameManager gameManager, final EditorManager editorManager) {
        this.arenaManager = arenaManager;
        this.gameManager = gameManager;
        this.editorManager = editorManager;
        this.lang = gameManager.getLang();
    }

    /**
     * Verifica se um jogador deve ser protegido de interagir com o mundo da arena.
     * <p>
     * Um jogador <b>não</b> é protegido se:
     * <ul>
     *   <li>Estiver em uma partida ativa ({@link GameManager#isInGame(Player)})</li>
     *   <li>Não estiver em um mundo de arena (nome do mundo não começa com "bw_")</li>
     *   <li>Estiver no modo edição da arena atual</li>
     * </ul>
     * </p>
     *
     * @param player o jogador a ser verificado (não nulo)
     * @return {@code true} se o jogador deve ser protegido, {@code false} caso contrário
     */
    private boolean shouldProtect(final Player player) {
        if (this.gameManager.isInGame(player)) {
            return false;
        }
        final Arena arena = this.getArena(player);
        if (arena == null) {
            return false;
        }
        if (this.editorManager.isEditing(player, arena.getName())) {
            return false;
        }
        return true;
    }

    /**
     * Obtém a arena associada ao mundo em que o jogador se encontra.
     * <p>
     * O nome do mundo deve começar com o prefixo "bw_". O restante do nome é usado
     * como identificador da arena.
     * </p>
     *
     * @param player o jogador (não nulo)
     * @return a arena correspondente ao mundo do jogador, ou {@code null} se o mundo
     *         não for um mundo de arena ou a arena não estiver registrada
     */
    private @Nullable Arena getArena(final Player player) {
        final String worldName = player.getWorld().getName();
        if (!worldName.startsWith("bw_")) {
            return null;
        }
        final String arenaName = worldName.substring(3);
        return this.arenaManager.get(arenaName);
    }

    /**
     * Manipula o evento de quebra de bloco.
     * <p>
     * Se o jogador estiver protegido, o evento é cancelado.
     * Se o jogador estiver no modo edição, tenta restaurar o spawn da arena,
     * o spawn do time, a cama ou o gerador correspondente ao bloco quebrado.
     * </p>
     *
     * @param event o evento de quebra de bloco (não nulo)
     */
    @EventHandler
    public void onBlockBreak(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
            return;
        }
        final Arena arena = this.getArena(player);
        if (arena == null) return;
        if (!this.editorManager.isEditing(player, arena.getName())) return;

        final Block block = event.getBlock();
        if (this.tryRestoreArenaSpawn(arena, block, event)) return;
        if (this.tryRestoreTeamSpawn(arena, block, event)) return;
        if (this.tryRestoreBed(arena, block, event)) return;
        this.tryRestoreGenerator(arena, block, event);
    }

    private boolean tryRestoreArenaSpawn(final Arena arena, final Block block, final BlockBreakEvent event) {
        if (arena.getArenaSpawn() == null || arena.getSpawnBlockData() == null) return false;
        final Location spawnBlock = arena.getArenaSpawn().getBlock().getRelative(0, -1, 0).getLocation();
        if (!this.isSameBlock(spawnBlock, block.getLocation())) return false;
        event.setCancelled(true);
        event.setDropItems(false);
        block.setBlockData(org.bukkit.Bukkit.createBlockData(arena.getSpawnBlockData()), false);
        arena.setArenaSpawn(null);
        arena.setSpawnBlockData(null);
        this.arenaManager.save(arena);
        block.getWorld().getPlayers().stream()
                .filter(p -> p.getWorld().equals(block.getWorld()))
                .forEach(p -> p.sendMessage(Component.text("Spawn da arena removido!", NamedTextColor.YELLOW)));
        return true;
    }

    private boolean tryRestoreTeamSpawn(final Arena arena, final Block block, final BlockBreakEvent event) {
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() == null || team.getSpawnBlockData() == null) continue;
            final Location markerLoc = team.getSpawn().getBlock().getRelative(0, -1, 0).getLocation();
            if (!this.isSameBlock(markerLoc, block.getLocation())) continue;
            event.setCancelled(true);
            event.setDropItems(false);
            block.setBlockData(org.bukkit.Bukkit.createBlockData(team.getSpawnBlockData()), false);
            team.setSpawn(null);
            team.setSpawnBlockData(null);
            this.arenaManager.save(arena);
            block.getWorld().getPlayers().stream()
                    .filter(p -> p.getWorld().equals(block.getWorld()))
                    .forEach(p -> p.sendMessage(Component.text("Spawn do time " + team.getName() + " removido!", NamedTextColor.YELLOW)));
            return true;
        }
        return false;
    }

    private boolean tryRestoreBed(final Arena arena, final Block block, final BlockBreakEvent event) {
        if (!(block.getBlockData() instanceof final Bed bedData)) return false;

        // Normalise to foot location regardless of which part was broken
        final Location clickedLoc = block.getLocation();
        final Location footLoc;
        if (bedData.getPart() == Bed.Part.HEAD) {
            final org.bukkit.block.BlockFace facing = bedData.getFacing();
            footLoc = clickedLoc.clone().add(
                    -facing.getModX(), -facing.getModY(), -facing.getModZ());
        } else {
            footLoc = clickedLoc;
        }

        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getBed() == null) continue;
            if (!this.isSameBlock(team.getBed(), footLoc)) continue;

            event.setCancelled(true);
            event.setDropItems(false);
            // Remove both bed blocks
            footLoc.getBlock().setType(Material.AIR, false);
            // Calculate head position from facing stored in team
            if (team.getBedFacing() != null) {
                try {
                    final org.bukkit.block.BlockFace face = org.bukkit.block.BlockFace.valueOf(team.getBedFacing().toUpperCase());
                    final Location headLoc = footLoc.clone().add(face.getModX(), face.getModY(), face.getModZ());
                    headLoc.getBlock().setType(Material.AIR, false);
                } catch (final IllegalArgumentException ignored) { }
            }
            team.setBed(null);
            team.setBedFacing(null);
            this.arenaManager.save(arena);
            block.getWorld().getPlayers().stream()
                    .filter(p -> p.getWorld().equals(block.getWorld()))
                    .forEach(p -> p.sendMessage(Component.text(
                            "Cama do time " + team.getName() + " removida!", NamedTextColor.YELLOW)));
            return true;
        }
        return false;
    }

    /**
     * Impede que jogadores em modo edição cliquem com botão direito em camas configuradas.
     * Isso evita que jogadores tentem dormir nas camas durante a edição.
     */
    @EventHandler
    public void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block block = event.getClickedBlock();
        if (block == null || !(block.getBlockData() instanceof Bed)) return;
        
        final Player player = event.getPlayer();
        final Arena arena = this.getArena(player);
        if (arena == null) return;
        
        // Se estiver em modo edição e a cama pertence a algum time da arena, cancela
        if (this.editorManager.isEditing(player, arena.getName())) {
            for (final ArenaTeam team : arena.getTeams()) {
                if (team.getBed() == null) continue;
                final Location footLoc = this.getBedFootLocation(block);
                if (footLoc != null && this.isSameBlock(team.getBed(), footLoc)) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("Use /bw admin arena setbed para configurar esta cama!", NamedTextColor.YELLOW));
                    return;
                }
            }
        }
    }

    private Location getBedFootLocation(final Block bedBlock) {
        if (!(bedBlock.getBlockData() instanceof final Bed bedData)) return null;
        final Location clickedLoc = bedBlock.getLocation();
        if (bedData.getPart() == Bed.Part.HEAD) {
            final org.bukkit.block.BlockFace facing = bedData.getFacing();
            return clickedLoc.clone().add(-facing.getModX(), -facing.getModY(), -facing.getModZ());
        }
        return clickedLoc;
    }

    private void tryRestoreGenerator(final Arena arena, final Block block, final BlockBreakEvent event) {
        final List<ArenaGenerator> gens = arena.getGenerators();
        for (int i = 0; i < gens.size(); i++) {
            final ArenaGenerator gen = gens.get(i);
            final Location loc = gen.getLocation();
            final Location below = loc.getBlock().getRelative(0, -1, 0).getLocation();
            if (this.isSameBlock(loc, block.getLocation()) || this.isSameBlock(below, block.getLocation())) {
                event.setCancelled(true);
                event.setDropItems(false);
                if (gen.getOriginBlockData() != null) {
                    below.getBlock().setBlockData(org.bukkit.Bukkit.createBlockData(gen.getOriginBlockData()), false);
                }
                arena.getGenerators().remove(i);
                this.arenaManager.save(arena);
                block.getWorld().getPlayers().stream()
                        .filter(p -> p.getWorld().equals(block.getWorld()))
                        .forEach(p -> p.sendMessage(Component.text("Gerador de " + gen.getType() + " removido!", NamedTextColor.YELLOW)));
                return;
            }
        }
    }

    /**
     * Verifica se duas localizações referem-se ao mesmo bloco (mesmo mundo e coordenadas).
     *
     * @param a primeira localização (não nula)
     * @param b segunda localização (não nula)
     * @return {@code true} se ambas as localizações apontam para o mesmo bloco
     */
    private boolean isSameBlock(final Location a, final Location b) {
        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    /**
     * Manipula o evento de colocação de bloco.
     * <p>
     * Se o jogador estiver protegido, o evento é cancelado.
     * </p>
     *
     * @param event o evento de colocação de bloco (não nulo)
     */
    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Manipula o evento de dano a entidades.
     * <p>
     * Se a entidade atingida for um jogador protegido, o dano é cancelado.
     * </p>
     *
     * @param event o evento de dano (não nulo)
     */
    @EventHandler
    public void onEntityDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Manipula o evento de dano causado por uma entidade a outra.
     * <p>
     * Se a entidade atingida for um jogador protegido, o dano é cancelado.
     * </p>
     *
     * @param event o evento de dano por entidade (não nulo)
     */
    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Manipula o evento de alteração do nível de fome.
     * <p>
     * Se a entidade for um jogador protegido, a alteração da fome é cancelada.
     * </p>
     *
     * @param event o evento de alteração de fome (não nulo)
     */
    @EventHandler
    public void onFoodLevelChange(final FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
        }
    }

    /**
     * Manipula o evento de execução de comando por um jogador.
     * <p>
     * Comandos que iniciam com "/bw " são permitidos. Todos os outros comandos
     * são bloqueados para jogadores protegidos, e uma mensagem de erro é exibida.
     * </p>
     *
     * @param event o evento de comando (não nulo)
     */
    @EventHandler
    public void onPlayerCommand(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String cmd = event.getMessage().toLowerCase();
        if (cmd.startsWith("/bw ")) {
            return;
        }
        if (this.shouldProtect(player)) {
            event.setCancelled(true);
            player.sendMessage(this.lang.text(NamedTextColor.RED, "commands_blocked"));
        }
    }

    /**
     * Manipula o evento de saída do jogador do servidor.
     * <p>
     * Se o jogador estiver no modo de edição de uma arena, encerra a sessão
     * para não deixar a arena bloqueada enquanto o jogador está offline.
     * </p>
     *
     * @param event o evento de saída (não nulo)
     */
    @EventHandler
    public void onPlayerQuit(final org.bukkit.event.player.PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final String arenaName = this.editorManager.getPlayerArena(player);
        if (arenaName != null) {
            this.editorManager.endSession(player);
            final Arena arena = this.arenaManager.get(arenaName);
            if (arena != null) {
                this.arenaManager.save(arena);
            }
        }
    }

    /**
     * Ao reconectar, se o jogador estiver em um mundo de arena (bw_*) sem
     * estar em partida nem em sessão de edição, teleporta para o lobby global
     * ou para o spawn do mundo principal, evitando que fique preso.
     */
    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String worldName = player.getWorld().getName();

        if (!worldName.startsWith("bw_")) return;
        if (this.gameManager.isInGame(player)) return;

        final Arena arena = this.getArena(player);
        if (arena != null && this.editorManager.isEditing(player, arena.getName())) return;

        // Player reconnected into an arena world without being in a game — rescue them
        final org.bukkit.Location lobby = this.gameManager.getConfigManager().getLobby();
        final org.bukkit.Location destination = lobby != null
                ? lobby
                : org.bukkit.Bukkit.getWorlds().get(0).getSpawnLocation();

        // Delay by 1 tick so the player is fully loaded before teleporting
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                this.gameManager.getPlugin(), () -> {
                    if (player.isOnline()) {
                        player.teleport(destination);
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    }
                }, 1L);
    }

    /**
     * Cancela o jogador dormir em camas — tanto em modo edição quanto em partidas.
     */
    @EventHandler
    public void onPlayerBedEnter(final org.bukkit.event.player.PlayerBedEnterEvent event) {
        final Player player = event.getPlayer();
        final String worldName = player.getWorld().getName();

        // Bloqueia em mundos de edição (bw_*)
        if (worldName.startsWith("bw_")) {
            event.setCancelled(true);
            return;
        }

        // Bloqueia em mundos de arena (jogadores em partida)
        if (this.gameManager.getPlayerGame(player) != null) {
            event.setCancelled(true);
            return;
        }

        // Bloqueia em qualquer mundo de arena carregado
        for (final Arena arena : this.arenaManager.getAll()) {
            if (worldName.equals(arena.getWorldName())) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
