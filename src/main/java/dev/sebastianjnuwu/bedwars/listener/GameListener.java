package dev.sebastianjnuwu.bedwars.listener;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Egg;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

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
import dev.sebastianjnuwu.bedwars.util.LocationUtil;

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

    private static final int BRIDGE_EGG_LENGTH = 16;
    private static final int IRON_GOLEM_RANGE = 20;
    private static final GoalKey<IronGolem> GOLEM_ATTACK_GOAL_KEY = GoalKey.of(IronGolem.class,
            new NamespacedKey("bedwars", "golem_attack"));

    private final GameManager gameManager;
    private final LangManager lang;
    private final Map<UUID, ArenaTeam> golemOwners;

    /**
     * Constrói um novo {@code GameListener}.
     *
     * @param gameManager gerenciador de partidas (não nulo)
     */
    public GameListener(final GameManager gameManager) {
        this.gameManager = gameManager;
        this.lang = gameManager.getLang();
        this.golemOwners = new HashMap<>();
        Bukkit.getScheduler().runTaskTimer(gameManager.getPlugin(), this::tickIronGolems, 10L, 10L);
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
            // Reafirma a posição no tick seguinte: garante que o jogador nasça no
            // spawn do time mesmo se outro plugin sobrescrever a localização do
            // respawn em prioridade mais alta (HIGHEST/MONITOR).
            final Location reassert = target.clone();
            Bukkit.getScheduler().runTask(this.gameManager.getPlugin(), () -> {
                if (player.isOnline() && this.gameManager.getPlayerGame(player) == game) {
                    player.teleport(reassert);
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
     * Lança uma bola de fogo ({@link SmallFireball}) ao usar o item
     * {@code FIRE_CHARGE} da loja durante uma partida.
     * <p>
     * Sem esse tratamento, o item cai no comportamento padrão de ignição
     * (funciona como um isqueiro) em vez de disparar o projétil.
     * </p>
     *
     * @param event o evento de interação (não nulo)
     */
    @EventHandler
    public void onFireballUse(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack item = this.usedItem(event, player);
        if (item == null || item.getType() != Material.FIRE_CHARGE) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null || !game.isPlaying(player)) {
            return;
        }
        event.setCancelled(true);
        final SmallFireball fireball = player.launchProjectile(
                SmallFireball.class, player.getLocation().getDirection().multiply(1.5));
        fireball.setShooter(player);
        this.consumeUsedItem(event, player, item);
    }

    /**
     * Aplica o impulso da bola de fogo ao jogador atingido (estilo Hypixel).
     * <p>
     * No impacto, a vítima é empurrada horizontalmente na direção do projétil
     * e lançada para o alto, criando o efeito de "quase voar" característico.
     * Apenas jogadores em partida ativa (PLAYING) recebem o impulso.
     * </p>
     *
     * @param event o evento de impacto do projétil (não nulo)
     */
    @EventHandler
    public void onFireballHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof final SmallFireball fireball)) {
            return;
        }
        if (event.getHitEntity() instanceof final Player victim) {
            this.knockbackVictim(fireball, victim);
            return;
        }
        if (event.getHitBlock() != null) {
            this.windBlast(fireball);
        }
    }

    /**
     * Aplica o impulso da bola de fogo ao jogador atingido (estilo Hypixel).
     * <p>
     * No impacto, a vítima é empurrada horizontalmente na direção do projétil
     * e lançada para o alto, criando o efeito de "quase voar" característico.
     * Apenas jogadores em partida ativa (PLAYING) recebem o impulso.
     * </p>
     *
     * @param fireball o projétil que atingiu o jogador (não nulo)
     * @param victim   o jogador atingido (não nulo)
     */
    private void knockbackVictim(final SmallFireball fireball, final Player victim) {
        final Game game = this.gameManager.getPlayerGame(victim);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        final Vector dir = fireball.getVelocity().clone();
        dir.setY(0);
        if (dir.lengthSquared() < 0.0001) {
            return;
        }
        dir.normalize().multiply(2.2).setY(1.0);
        victim.setVelocity(victim.getVelocity().add(dir));
    }

    /**
     * Aplica o efeito de "vento" (estilo Wind Charge) quando a fireball acerta
     * um bloco.
     * <p>
     * Empurra radialmente todos os jogadores da partida em um raio do ponto de
     * impacto (para longe da explosão, como o vento), e o atirador recebe um
     * impulso vertical extra (super pulo). Apenas em partida ativa.
     * </p>
     *
     * @param fireball o projétil que atingiu o bloco (não nulo)
     */
    private void windBlast(final SmallFireball fireball) {
        if (!(fireball.getShooter() instanceof final Player shooter)) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(shooter);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        final Location center = fireball.getLocation();
        for (final Player p : game.getPlayers()) {
            final Player online = Bukkit.getPlayer(p.getUniqueId());
            if (online == null || !online.getWorld().equals(center.getWorld())) {
                continue;
            }
            final double dist = online.getLocation().distance(center);
            if (dist > 5.0) {
                continue;
            }
            Vector push = online.getLocation().toVector().subtract(center.toVector());
            push.setY(0);
            if (push.lengthSquared() < 0.0001) {
                push = online.getLocation().getDirection().clone().multiply(-1);
            }
            push.normalize().multiply(2.2 * (1.0 - dist / 5.0) + 0.6).setY(1.0);
            online.setVelocity(online.getVelocity().add(push));
        }
    }

    /**
     * Cria a ponte de lã do ovo de ponte ({@code EGG}) ao usar o item.
     * <p>
     * Estilo Hypixel: o ovo é lançado como projétil e, ao pousar, cria uma ponte
     * de lã conectando o ponto de impacto ao atirador, subindo/descendo conforme
     * a trajetória. A lã usa a cor do time do atirador. Cada bloco é rastreado
     * via {@link Game#trackPlacedBlock(org.bukkit.Location)} para ser limpo no reset.
     * </p>
     *
     * @param event o evento de interação (não nulo)
     */
    @EventHandler
    public void onBridgeEggUse(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack item = this.usedItem(event, player);
        if (item == null || item.getType() != Material.EGG) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        event.setCancelled(true);
        final Egg egg = player.launchProjectile(
                Egg.class, player.getLocation().getDirection().multiply(1.5));
        egg.setShooter(player);
        this.consumeUsedItem(event, player, item);
    }

    /**
     * Cria a ponte quando o ovo de ponte pousa.
     * <p>
     * Interpola blocos de lã entre o atirador e o ponto de impacto (até
     * {@link #BRIDGE_EGG_LENGTH} blocos), acompanhando a altura da trajetória.
     * </p>
     *
     * @param event o evento de impacto do projétil (não nulo)
     */
    @EventHandler
    public void onBridgeEggHit(final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof final Egg egg)) {
            return;
        }
        if (!(egg.getShooter() instanceof final Player shooter)) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(shooter);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        final Location impact = event.getHitBlock() != null
                ? event.getHitBlock().getLocation().add(0.5, 1, 0.5)
                : egg.getLocation();
        final World world = egg.getWorld();
        final Location start = shooter.getLocation();
        final Vector delta = impact.toVector().subtract(start.toVector());
        final int length = Math.min(BRIDGE_EGG_LENGTH, (int) delta.length());
        if (length <= 0) {
            return;
        }
        final Vector step = delta.normalize();
        final ArenaTeam team = game.getPlayerTeam(shooter);
        final Material wool = team != null ? getWoolColor(team.getColor()) : Material.WHITE_WOOL;
        for (int i = 1; i <= length; i++) {
            final Vector point = start.toVector().add(step.clone().multiply(i));
            final Block target = world.getBlockAt(point.getBlockX(), point.getBlockY(), point.getBlockZ());
            if (!target.getType().isAir()) {
                continue;
            }
            target.setType(wool);
            game.trackPlacedBlock(target.getLocation());
        }
    }

    /**
     * Convoca um golem de ferro ({@code IRON_GOLEM_SPAWN_EGG}) ao usar o item
     * da loja durante uma partida.
     * <p>
     * O golem nasce na posição do jogador, fica associado ao time do atirador
     * e passa a defender a área (ver {@link #onGolemDeath} e a IA de alvo).
     * </p>
     *
     * @param event o evento de interação (não nulo)
     */
    @EventHandler
    public void onIronGolemUse(final PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        final Player player = event.getPlayer();
        final ItemStack item = this.usedItem(event, player);
        if (item == null || item.getType() != Material.IRON_GOLEM_SPAWN_EGG) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null || game.getState() != GameState.PLAYING) {
            return;
        }
        final ArenaTeam team = game.getPlayerTeam(player);
        if (team == null) {
            return;
        }
        event.setCancelled(true);
        final Location spawn = player.getLocation().clone();
        final IronGolem golem = player.getWorld().spawn(spawn, IronGolem.class);
        golem.setPlayerCreated(false);
        golem.customName(this.lang.text(NamedTextColor.GREEN, "game.iron_golem_name", team.getName().toUpperCase()));
        golem.setCustomNameVisible(true);
        golem.setPersistent(true);
        Bukkit.getMobGoals().addGoal(golem, 0, new GolemAttackGoal(golem, team));
        this.golemOwners.put(golem.getUniqueId(), team);
        player.sendMessage(this.lang.text(NamedTextColor.GREEN, "game.iron_golem_spawned"));
        this.consumeUsedItem(event, player, item);
    }

    /**
     * Controla a morte de golems de ferro convocados em partida.
     * <p>
     * Remove o golem do registro para que a IA pare de mirá-lo e impede que
     * ele solte ferro ou papoilas ao morrer.
     * </p>
     *
     * @param event o evento de morte (não nulo)
     */
    @EventHandler
    public void onGolemDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof final IronGolem golem)) {
            return;
        }
        this.golemOwners.remove(golem.getUniqueId());
        event.getDrops().clear();
    }

    /**
     * Impede dano amigável envolvendo golems de ferro convocados.
     * <p>
     * Um golem não pode danificar o jogador que o convocou nem aliados, e
     * jogadores do mesmo time não podem danificar o golem. Dano de inimigos
     * é mantido normalmente.
     * </p>
     *
     * @param event o evento de dano por entidade (não nulo)
     */
    @EventHandler
    public void onGolemDamage(final EntityDamageByEntityEvent event) {
        final IronGolem golem;
        final Player player;
        if (event.getDamager() instanceof final IronGolem damagerGolem
                && event.getEntity() instanceof final Player victim) {
            golem = damagerGolem;
            player = victim;
        } else if (event.getEntity() instanceof final IronGolem victimGolem
                && event.getDamager() instanceof final Player attacker) {
            golem = victimGolem;
            player = attacker;
        } else {
            return;
        }
        final ArenaTeam ownerTeam = this.golemOwners.get(golem.getUniqueId());
        if (ownerTeam == null) {
            return;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return;
        }
        final ArenaTeam playerTeam = game.getPlayerTeam(player);
        if (playerTeam != null && playerTeam.getName().equals(ownerTeam.getName())) {
            event.setCancelled(true);
        }
    }

    /**
     * Remove do registro golems que saíram do mundo ou não são mais válidos.
     * <p>
     * O alvo e a perseguição são controlados pela {@link GolemAttackGoal}
     * registrada em cada golem no momento da convocação; este tick periódico
     * apenas evita o acúmulo de entradas órfãs no mapa de donos.
     * </p>
     */
    public void tickIronGolems() {
        this.golemOwners.entrySet().removeIf(entry -> {
            final org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
            return !(entity instanceof final IronGolem golem) || !golem.isValid();
        });
    }

    /**
     * IA que faz um golem de ferro convocado perseguir e atacar o inimigo mais
     * próximo.
     * <p>
     * Contorna a restrição vanilla de golems criados por jogador, que não
     * perseguem jogadores ({@code canTarget(Player)} é falso). Em vez de
     * depender do {@code setTarget}, a goal move o golem com o
     * {@code Pathfinder} e aplica o dano via {@code attack} diretamente,
     * usando um cooldown próprio para não repetir o golpe a cada tick.
     * </p>
     */
    private final class GolemAttackGoal implements Goal<IronGolem> {

        private static final int ATTACK_COOLDOWN = 20;
        private static final double CHASE_SPEED = 1.0D;
        private static final double ATTACK_RANGE_SQ = 4.0D;

        private final IronGolem golem;
        private final ArenaTeam ownerTeam;
        private @Nullable Player target;
        private int attackCooldown;

        GolemAttackGoal(final IronGolem golem, final ArenaTeam ownerTeam) {
            this.golem = golem;
            this.ownerTeam = ownerTeam;
        }

        @Override
        public boolean shouldActivate() {
            final Game game = this.currentGame();
            if (game == null) {
                return false;
            }
            this.target = findNearestEnemy(this.golem, game, this.ownerTeam);
            return this.target != null;
        }

        @Override
        public boolean shouldStayActive() {
            return this.target != null && this.target.isValid() && !this.target.isDead()
                    && this.golem.getWorld() == this.target.getWorld();
        }

        @Override
        public void start() {
            this.golem.setTarget(this.target);
        }

        @Override
        public void tick() {
            final Game game = this.currentGame();
            if (game == null) {
                return;
            }
            final Player current = findNearestEnemy(this.golem, game, this.ownerTeam);
            if (current != null) {
                this.target = current;
            }
            if (this.target == null) {
                return;
            }
            this.golem.setTarget(this.target);
            final double distanceSq = this.golem.getLocation().distanceSquared(this.target.getLocation());
            if (distanceSq <= ATTACK_RANGE_SQ) {
                this.golem.lookAt(this.target);
                if (this.attackCooldown <= 0) {
                    this.golem.attack(this.target);
                    this.attackCooldown = ATTACK_COOLDOWN;
                }
            } else {
                this.golem.getPathfinder().moveTo(this.target, CHASE_SPEED);
            }
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
        }

        @Override
        public void stop() {
            this.golem.setTarget(null);
        }

        @Override
        public GoalKey<IronGolem> getKey() {
            return GOLEM_ATTACK_GOAL_KEY;
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.TARGET);
        }

        private @Nullable Game currentGame() {
            return GameListener.this.gameManager.getGameByWorld(this.golem.getWorld().getName());
        }
    }

    /**
     * Encontra o inimigo vivo mais próximo do golem dentro do alcance.
     *
     * @param golem     o golem de ferro (não nulo)
     * @param game      a partida (não nula)
     * @param ownerTeam time dono do golem (não nulo)
     * @return o inimigo mais próximo ou {@code null} se não houver
     */
    private @Nullable Player findNearestEnemy(final IronGolem golem, final Game game, final ArenaTeam ownerTeam) {
        Player nearest = null;
        double nearestDistanceSq = IRON_GOLEM_RANGE * (double) IRON_GOLEM_RANGE;
        for (final Player candidate : game.getPlayers()) {
            if (!game.isPlaying(candidate)) {
                continue;
            }
            if (candidate.getWorld() != golem.getWorld()) {
                continue;
            }
            final ArenaTeam candidateTeam = game.getPlayerTeam(candidate);
            if (candidateTeam == null || candidateTeam.getName().equals(ownerTeam.getName())) {
                continue;
            }
            final double distanceSq = golem.getLocation().distanceSquared(candidate.getLocation());
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /**
     * Retorna o item usado na interação, respeitando a mão (principal ou offhand).
     * <p>
     * O {@code PlayerInteractEvent} reporta via {@code getHand()} qual das duas
     * mãos iniciou a ação; {@code getItem()} sozinho pode devolver o item errado
     * quando o item está na mão secundária (ex.: slot do escudo). Este helper
     * lê o slot correto conforme a mão do evento.
     * </p>
     *
     * @param event  o evento de interação (não nulo)
     * @param player o jogador (não nulo)
     * @return o item usado ou {@code null} se a mão não tiver item
     */
    private @Nullable ItemStack usedItem(final PlayerInteractEvent event, final Player player) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return event.getItem();
    }

    /**
     * Consome uma unidade do item usado, respeitando a mão da interação.
     * <p>
     * Diminui a quantidade do slot correto (principal ou offhand), removendo o
     * item por completo quando a pilha chega a zero.
     * </p>
     *
     * @param event  o evento de interação (não nulo)
     * @param player o jogador (não nulo)
     * @param item   o item a ser consumido (não nulo)
     */
    private void consumeUsedItem(final PlayerInteractEvent event, final Player player, final ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(null);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }

    private static Material getWoolColor(final String dyeColor) {
        return switch (dyeColor.toUpperCase()) {
            case "RED", "VERMELHO" -> Material.RED_WOOL;
            case "BLUE", "AZUL" -> Material.BLUE_WOOL;
            case "GREEN", "VERDE" -> Material.GREEN_WOOL;
            case "YELLOW", "AMARELO" -> Material.YELLOW_WOOL;
            case "PURPLE", "ROXO" -> Material.PURPLE_WOOL;
            case "PINK", "ROSA" -> Material.PINK_WOOL;
            case "ORANGE", "LARANJA" -> Material.ORANGE_WOOL;
            case "CYAN", "CIANO" -> Material.CYAN_WOOL;
            case "LIME" -> Material.LIME_WOOL;
            case "LIGHT_BLUE", "AZUL_CLARO" -> Material.LIGHT_BLUE_WOOL;
            case "GRAY", "CINZA" -> Material.GRAY_WOOL;
            case "BLACK", "PRETO" -> Material.BLACK_WOOL;
            default -> Material.WHITE_WOOL;
        };
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
