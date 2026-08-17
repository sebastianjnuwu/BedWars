package dev.sebastianjnuwu.bedwars.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.manager.GameManager;

/**
 * Listener responsável pelos itens especiais e inventário da partida de BedWars.
 * <p>
 * Gerencia a fireball da loja, o ovo de ponte, o drop de itens, a proteção de
 * armaduras no inventário, a coleta de itens e o bloqueio de crafting.
 * </p>
 */
public class GameItemListener implements Listener {

    private static final int BRIDGE_EGG_LENGTH = 16;

    private final GameManager gameManager;

    public GameItemListener(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

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
            final Player online = org.bukkit.Bukkit.getPlayer(p.getUniqueId());
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

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPlayerPickupItem(final PlayerPickupItemEvent event) {
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

    private static boolean isArmorPiece(final Material material) {
        final String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
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
}