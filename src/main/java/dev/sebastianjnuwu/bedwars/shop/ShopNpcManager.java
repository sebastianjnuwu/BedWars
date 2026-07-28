package dev.sebastianjnuwu.bedwars.shop;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Manages the lifecycle of shop NPCs for BedWars arenas.
 * <p>
 * NPCs are spawned programmatically via the FancyNPCs API when a game starts
 * and removed when the game ends. They are never persisted to disk because
 * arena worlds are reset after each game.
 */
public class ShopNpcManager {

    private final JavaPlugin plugin;
    private final Map<String, List<Npc>> activeNpcs;

    /**
     * Creates a new ShopNpcManager.
     *
     * @param plugin the plugin instance
     */
    public ShopNpcManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.activeNpcs = new HashMap<>();
    }

    /**
     * Spawns shop NPCs for an arena at the given locations.
     * <p>
     * Each NPC is named {@code bw-shop-<arena>-<index>}, registered with
     * FancyNPCs, and spawned for all players. NPCs are not saved to disk.
     *
     * @param arenaName the arena identifier
     * @param locations the locations where NPCs should be spawned
     * @param skin      the skin to apply (username or null for default)
     */
    public void spawnShopNpcs(final String arenaName, final List<Location> locations, final String skin) {
        if (locations == null || locations.isEmpty()) return;

        try {
            Class.forName("de.oliver.fancynpcs.api.FancyNpcsPlugin");
        } catch (final ClassNotFoundException e) {
            plugin.getLogger().warning("FancyNPCs not installed, cannot spawn shop NPCs");
            return;
        }

        final List<Npc> npcs = new ArrayList<>();
        final UUID creator = UUID.randomUUID();

        for (int i = 0; i < locations.size(); i++) {
            final Location loc = locations.get(i);
            if (loc == null || loc.getWorld() == null) continue;

            final String npcName = "bw-shop-" + arenaName + "-" + i;
            final NpcData data = new NpcData(npcName, creator, loc);
            data.setSkin(skin != null ? skin : "NPC");
            data.setDisplayName("<red>Loja</red>");

            final Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
            npc.setSaveToFile(false);
            FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
            npc.create();
            npc.spawnForAll();
            npcs.add(npc);
        }

        if (!npcs.isEmpty()) {
            activeNpcs.put(arenaName, npcs);
        }
    }

    /**
     * Removes all shop NPCs for the given arena.
     * <p>
     * Unregisters the NPCs from FancyNPCs and removes them from all players' view.
     *
     * @param arenaName the arena whose NPCs should be removed
     */
    public void removeShopNpcs(final String arenaName) {
        final List<Npc> npcs = activeNpcs.remove(arenaName);
        if (npcs == null) return;

        for (final Npc npc : npcs) {
            try {
                FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
                npc.removeForAll();
            } catch (final Exception e) {
                plugin.getLogger().warning("Failed to remove shop NPC: " + e.getMessage());
            }
        }
    }

    /**
     * Removes all shop NPCs across all arenas.
     * <p>
     * Called during plugin shutdown to ensure no NPCs remain.
     */
    public void removeAll() {
        for (final String key : new HashSet<>(activeNpcs.keySet())) {
            removeShopNpcs(key);
        }
    }
}
