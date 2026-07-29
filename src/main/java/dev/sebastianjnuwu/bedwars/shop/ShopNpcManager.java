package dev.sebastianjnuwu.bedwars.shop;

import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcData;
import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * Gerencia o ciclo de vida dos NPCs da loja para as arenas do BedWars.
 * <p>
 * Os NPCs sao spawnados via API do FancyNPCs em dois contextos:
 * <ul>
 *   <li><b>Partida</b> — spawnados quando a partida inicia, removidos ao final</li>
 *   <li><b>Edicao</b> — spawnados quando um admin edita a arena, visiveis no mundo</li>
 * </ul>
 * Os NPCs nunca sao persistidos pelo FancyNPCs ({@code setSaveToFile(false)});
 * seus dados sao armazenados no arquivo YAML da arena.
 */
public class ShopNpcManager {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final Map<String, List<Npc>> gameNpcs;
    private final Map<String, List<Npc>> editorNpcs;

    /**
     * Cria um novo gerenciador de NPCs da loja.
     *
     * @param plugin a instancia do plugin
     */
    public ShopNpcManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.gameNpcs = new HashMap<>();
        this.editorNpcs = new HashMap<>();
    }

    /**
     * Spawna os NPCs da loja para uma partida.
     *
     * @param arenaName identificador da arena
     * @param locations localizacoes dos NPCs
     * @param skin      nome da skin ou null para padrao
     */
    public void spawnGameNpcs(final String arenaName, final List<Location> locations, final String skin) {
        final List<Npc> npcs = spawnNpcs(arenaName, locations, skin, "jogo");
        if (npcs != null) {
            gameNpcs.put(arenaName, npcs);
        }
    }

    /**
     * Remove os NPCs da loja de uma partida.
     *
     * @param arenaName arena cujos NPCs serao removidos
     */
    public void removeGameNpcs(final String arenaName) {
        final List<Npc> npcs = gameNpcs.remove(arenaName);
        if (npcs != null) {
            removeNpcs(npcs);
        }
    }

    /**
     * Spawna os NPCs da loja para o modo edicao.
     *
     * @param arenaName identificador da arena
     * @param locations localizacoes dos NPCs
     * @param skin      nome da skin ou null para padrao
     */
    public void spawnEditorNpcs(final String arenaName, final List<Location> locations, final String skin) {
        removeEditorNpcs(arenaName);
        final List<Npc> npcs = spawnNpcs(arenaName, locations, skin, "editor");
        if (npcs != null) {
            editorNpcs.put(arenaName, npcs);
        }
    }

    /**
     * Remove os NPCs da loja do modo edicao.
     *
     * @param arenaName arena cujos NPCs serao removidos
     */
    public void removeEditorNpcs(final String arenaName) {
        final List<Npc> npcs = editorNpcs.remove(arenaName);
        if (npcs != null) {
            removeNpcs(npcs);
        }
    }

    /**
     * Spawna um unico NPC imediatamente (usado pelo comando add).
     *
     * @param arenaName identificador da arena
     * @param index     indice deste NPC
     * @param location  local onde spawnar
     * @param skin      nome da skin ou null para padrao
     * @return o NPC spawnado, ou null se o FancyNPCs nao estiver disponivel
     */
    public @org.jetbrains.annotations.Nullable Npc spawnSingleNpc(final String arenaName, final int index,
                                                                   final Location location, final String skin) {
        if (location == null || location.getWorld() == null) return null;
        if (!checkFancyNpcs()) return null;

        final String npcName = "bw-shop-" + arenaName + "-" + index;
        final NpcData data = new NpcData(npcName, UUID.randomUUID(), location);
        data.setSkin(skin != null ? skin : "NPC");
        data.setDisplayName("<red>Loja</red>");

        final Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
        npc.setSaveToFile(false);
        FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
        npc.create();
        npc.spawnForAll();
        return npc;
    }

    /**
     * Remove todos os NPCs (partida e edicao) de todas as arenas.
     */
    public void removeAll() {
        for (final String key : new HashSet<>(gameNpcs.keySet())) {
            removeGameNpcs(key);
        }
        for (final String key : new HashSet<>(editorNpcs.keySet())) {
            removeEditorNpcs(key);
        }
    }

    // ── metodos internos ─────────────────────────────────────────────────

    private boolean checkFancyNpcs() {
        try {
            Class.forName("de.oliver.fancynpcs.api.FancyNpcsPlugin");
            return true;
        } catch (final ClassNotFoundException e) {
            plugin.getLogger().warning(this.lang.raw("log.shop_npc.not_installed"));
            return false;
        }
    }

    private @org.jetbrains.annotations.Nullable List<Npc> spawnNpcs(final String arenaName, final List<Location> locations,
                                                                      final String skin, final String context) {
        if (locations == null || locations.isEmpty()) return null;
        if (!checkFancyNpcs()) return null;

        final List<Npc> npcs = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            final Location loc = locations.get(i);
            if (loc == null || loc.getWorld() == null) continue;

            try {
                final String npcName = "bw-shop-" + arenaName + "-" + context + "-" + i;
                final NpcData data = new NpcData(npcName, UUID.randomUUID(), loc);
                data.setSkin(skin != null ? skin : "NPC");
                data.setDisplayName("<red>Loja</red>");

                final Npc npc = FancyNpcsPlugin.get().getNpcAdapter().apply(data);
                npc.setSaveToFile(false);
                FancyNpcsPlugin.get().getNpcManager().registerNpc(npc);
                npc.create();
                npc.spawnForAll();
                npcs.add(npc);
            } catch (final Exception e) {
                plugin.getLogger().warning(this.lang.raw("log.shop_npc.spawn_error", String.valueOf(i), arenaName, e.getMessage()));
            }
        }

        return npcs.isEmpty() ? null : npcs;
    }

    private void removeNpcs(final List<Npc> npcs) {
        for (final Npc npc : npcs) {
            try {
                FancyNpcsPlugin.get().getNpcManager().removeNpc(npc);
                npc.removeForAll();
            } catch (final Exception e) {
                plugin.getLogger().warning(this.lang.raw("log.shop_npc.remove_error", e.getMessage()));
            }
        }
    }
}
