package dev.sebastianjnuwu.bedwars.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.lang.LangManager;

public final class PlayerStateManager {

    private final LangManager lang;
    private final Logger logger;
    private final Map<UUID, PlayerStateSnapshot> snapshots = new HashMap<>();

    public PlayerStateManager(final JavaPlugin plugin, final LangManager lang) {
        this.lang = lang;
        this.logger = plugin.getLogger();
    }

    public void savePlayerState(final Player player) {
        final UUID uuid = player.getUniqueId();
        if (this.snapshots.containsKey(uuid)) {
            return;
        }
        this.snapshots.put(uuid, new PlayerStateSnapshot(player));
        this.logger.info(this.lang.raw("debug.player_inventory_saved", player.getName(), uuid.toString()));
    }

    public void restorePlayerState(final Player player) {
        final UUID uuid = player.getUniqueId();
        final PlayerStateSnapshot snapshot = this.snapshots.get(uuid);
        if (snapshot == null) {
            return;
        }
        try {
            snapshot.restore(player);
            this.snapshots.remove(uuid);
            this.logger.info(this.lang.raw("debug.player_inventory_restored", player.getName(), uuid.toString()));
        } catch (final Exception e) {
            this.logger.severe(this.lang.raw("debug.player_inventory_restore_failed", player.getName(), uuid.toString())
                    + ": " + e.getMessage());
        }
    }

    public boolean hasSavedState(final Player player) {
        return this.snapshots.containsKey(player.getUniqueId());
    }

    public void clearSavedState(final Player player) {
        this.snapshots.remove(player.getUniqueId());
    }

    public int getSnapshotCount() {
        return this.snapshots.size();
    }

    private static final class PlayerStateSnapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack[] extra;
        private final ItemStack[] enderChest;
        private final float exp;
        private final int level;
        private final GameMode gameMode;

        private PlayerStateSnapshot(final Player player) {
            this.contents = cloneItems(player.getInventory().getContents());
            this.armor = cloneItems(player.getInventory().getArmorContents());
            this.extra = cloneItems(player.getInventory().getExtraContents());
            this.enderChest = cloneItems(player.getEnderChest().getContents());
            this.exp = player.getExp();
            this.level = player.getLevel();
            this.gameMode = player.getGameMode();
        }

        private void restore(final Player player) {
            player.getInventory().clear();
            player.getInventory().setContents(this.contents);
            player.getInventory().setArmorContents(this.armor);
            player.getInventory().setExtraContents(this.extra);
            player.getEnderChest().setContents(this.enderChest);
            player.setExp(this.exp);
            player.setLevel(this.level);
            player.setGameMode(this.gameMode);
        }

        private static ItemStack[] cloneItems(final ItemStack[] items) {
            final ItemStack[] copy = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) {
                final ItemStack item = items[i];
                copy[i] = item == null ? null : item.clone();
            }
            return copy;
        }
    }
}
