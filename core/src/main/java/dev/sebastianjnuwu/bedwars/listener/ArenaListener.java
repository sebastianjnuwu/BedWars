package dev.sebastianjnuwu.bedwars.listener;

import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.arena.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.game.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

/**
 * Listener responsável por proteger arenas durante o modo edição e fora de partidas.
 * <p>
 * Garante que jogadores que não estão em uma partida ativa não possam interagir com o mundo
 * da arena (quebrar blocos, colocar blocos, sofrer dano, perder fome ou executar comandos).
 * Durante o modo edição ({@link EditorManager}), jogadores podem modificar livremente a arena.
 * A restauração de marcadores quebrados (spawn, cama, gerador) fica em {@link ArenaBlockRestorer}.
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
    private final ArenaBlockRestorer blockRestorer;

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
        this.blockRestorer = new ArenaBlockRestorer(this.arenaManager, this.lang);
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
        if (arena == null) {
            return;
        }
        if (!this.editorManager.isEditing(player, arena.getName())) {
            return;
        }
        this.blockRestorer.tryRestore(arena, event);
    }

    /**
     * Impede que jogadores em modo edição cliquem com botão direito em camas configuradas.
     * Isso evita que jogadores tentem dormir nas camas durante a edição.
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
        final Arena arena = this.getArena(player);
        if (arena == null) {
            return;
        }

        if (this.editorManager.isEditing(player, arena.getName())) {
            for (final ArenaTeam team : arena.getTeams()) {
                if (team.getBed() == null) {
                    continue;
                }
                final Location footLoc = this.blockRestorer.getBedFootLocation(block);
                if (footLoc != null && this.blockRestorer.isSameBlock(team.getBed(), footLoc)) {
                    event.setCancelled(true);
                    CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.YELLOW, "edit.setbed_hint"));
                    return;
                }
            }
        }
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
            CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.RED, "commands_blocked"));
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
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final String arenaName = this.editorManager.getPlayerArena(player);
        if (arenaName != null) {
            this.gameManager.getShopNpcManager().removeEditorNpcs(arenaName);
            this.editorManager.endSession(player);
            final Arena arena = this.arenaManager.get(arenaName);
            if (arena != null) {
                this.arenaManager.save(arena);
            }
        }
    }

    /**
     * Cancela o jogador dormir em camas — tanto em modo edição quanto em partidas.
     */
    @EventHandler
    public void onPlayerBedEnter(final PlayerBedEnterEvent event) {
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
}
