package dev.sebastianjnuwu.bedwars.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.format.NamedTextColor;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.compat.CompatProvider;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

/**
 * Listener responsável pelos golems de ferro convocados na partida de BedWars.
 * <p>
 * Gerencia a convocação via item da loja, o registro de dono, o dano amigável,
 * a IA de alvo e a limpeza periódica de entradas órfãs.
 * </p>
 */
public class GameGolemListener implements Listener {

    private static final int IRON_GOLEM_RANGE = 20;

    private final GameManager gameManager;
    private final LangManager lang;
    private final Map<UUID, ArenaTeam> golemOwners;

    public GameGolemListener(final GameManager gameManager) {
        this.gameManager = gameManager;
        this.lang = gameManager.getLang();
        this.golemOwners = new HashMap<>();
        Bukkit.getScheduler().runTaskTimer(gameManager.getPlugin(), this::tickIronGolems, 10L, 10L);
    }

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
        CompatProvider.golem().registerAttackGoal(golem, team, this::findNearestEnemyForGoal);
        this.golemOwners.put(golem.getUniqueId(), team);
        CompatProvider.chat().sendMessage(player, this.lang.text(NamedTextColor.GREEN, "game.iron_golem_spawned"));
        this.consumeUsedItem(event, player, item);
    }

    @EventHandler
    public void onGolemDeath(final EntityDeathEvent event) {
        if (!(event.getEntity() instanceof final IronGolem golem)) {
            return;
        }
        this.golemOwners.remove(golem.getUniqueId());
        event.getDrops().clear();
    }

    @EventHandler
    public void onGolemDamage(final EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof final IronGolem damagerGolem) {
            if (event.getEntity() instanceof final IronGolem victimGolem) {
                final ArenaTeam damagerTeam = this.golemOwners.get(damagerGolem.getUniqueId());
                final ArenaTeam victimTeam = this.golemOwners.get(victimGolem.getUniqueId());
                if (damagerTeam != null && damagerTeam.getName().equals(victimTeam.getName())) {
                    event.setCancelled(true);
                }
                return;
            }
            if (event.getEntity() instanceof final Player victim && this.isSameTeam(damagerGolem, victim)) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getEntity() instanceof final IronGolem victimGolem) {
            final Player attacker = this.attackerOf(event.getDamager());
            if (attacker != null && this.isSameTeam(victimGolem, attacker)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGolemTarget(final EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof final IronGolem golem)
                || !(event.getTarget() instanceof final Player target)) {
            return;
        }
        if (this.isSameTeam(golem, target)) {
            event.setCancelled(true);
        }
    }

    private @Nullable Player attackerOf(final Entity damager) {
        if (damager instanceof final Player player) {
            return player;
        }
        if (damager instanceof final Projectile projectile
                && projectile.getShooter() instanceof final Player shooter) {
            return shooter;
        }
        return null;
    }

    private boolean isSameTeam(final IronGolem golem, final Player player) {
        final ArenaTeam ownerTeam = this.golemOwners.get(golem.getUniqueId());
        if (ownerTeam == null) {
            return false;
        }
        final Game game = this.gameManager.getPlayerGame(player);
        if (game == null) {
            return false;
        }
        final ArenaTeam playerTeam = game.getPlayerTeam(player);
        return playerTeam != null && playerTeam.getName().equals(ownerTeam.getName());
    }

    public void tickIronGolems() {
        this.golemOwners.entrySet().removeIf(entry -> {
            final Entity entity = Bukkit.getEntity(entry.getKey());
            return !(entity instanceof final IronGolem golem) || !golem.isValid();
        });
    }

    private @Nullable LivingEntity findNearestEnemyForGoal(final IronGolem golem, final ArenaTeam ownerTeam) {
        final Game game = this.gameManager.getGameByWorld(golem.getWorld().getName());
        if (game == null) {
            return null;
        }
        return findNearestEnemy(golem, game, ownerTeam);
    }

    private @Nullable LivingEntity findNearestEnemy(final IronGolem golem, final Game game, final ArenaTeam ownerTeam) {
        LivingEntity nearest = null;
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
        for (final var entry : this.golemOwners.entrySet()) {
            if (entry.getValue().getName().equals(ownerTeam.getName())) {
                continue;
            }
            final Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof final IronGolem enemyGolem) || !enemyGolem.isValid()
                    || enemyGolem == golem || enemyGolem.getWorld() != golem.getWorld()) {
                continue;
            }
            final double distanceSq = golem.getLocation().distanceSquared(enemyGolem.getLocation());
            if (distanceSq < nearestDistanceSq) {
                nearestDistanceSq = distanceSq;
                nearest = enemyGolem;
            }
        }
        return nearest;
    }

    private @Nullable ItemStack usedItem(final PlayerInteractEvent event, final Player player) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        return event.getItem();
    }

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
}