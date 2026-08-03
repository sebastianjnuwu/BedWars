package dev.sebastianjnuwu.bedwars.shop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;
import dev.sebastianjnuwu.bedwars.hook.FancyNpcsHook;
import dev.sebastianjnuwu.bedwars.hook.NpcHook;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Gerencia o ciclo de vida dos NPCs da loja para as arenas do BedWars.
 * <p>
 * Os NPCs são criados via FancyNPCs quando o backend estiver disponível.
 */
public class ShopNpcManager {

    private final JavaPlugin plugin;
    private final LangManager lang;
    private final NpcHook npcHook;
    private final Map<String, List<Object>> gameNpcs;
    private final Map<String, List<Object>> editorNpcs;

    /**
     * Cria um novo gerenciador de NPCs da loja.
     *
     * @param plugin a instancia do plugin
     */
    public ShopNpcManager(final JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((BedWarsPlugin) plugin).getLang();
        this.npcHook = new FancyNpcsHook(plugin);
        this.gameNpcs = new HashMap<>();
        this.editorNpcs = new HashMap<>();
    }

    /**
     * Spawna os NPCs da loja para uma partida.
     *
     * @param arenaName identificador da arena
     * @param npcs      NPCs da loja a serem spawnados
     */
    public void spawnGameNpcs(final String arenaName, final List<ShopNpc> npcs) {
        this.removeEditorNpcs(arenaName);
        final List<Object> created = this.spawnNpcs(arenaName, npcs, "jogo");
        if (created != null) {
            this.gameNpcs.put(arenaName, created);
        }
    }

    /**
     * Remove os NPCs da loja de uma partida.
     *
     * @param arenaName arena cujos NPCs serao removidos
     */
    public void removeGameNpcs(final String arenaName) {
        final List<Object> npcs = this.gameNpcs.remove(arenaName);
        if (npcs != null) {
            this.removeNpcs(npcs);
        }
    }

    /**
     * Spawna os NPCs da loja para o modo edicao.
     *
     * @param arenaName identificador da arena
     * @param npcs      NPCs da loja a serem spawnados
     */
    public void spawnEditorNpcs(final String arenaName, final List<ShopNpc> npcs) {
        this.removeEditorNpcs(arenaName);
        final List<Object> created = this.spawnNpcs(arenaName, npcs, "editor");
        if (created != null) {
            this.editorNpcs.put(arenaName, created);
        }
    }

    /**
     * Remove os NPCs da loja do modo edicao.
     *
     * @param arenaName arena cujos NPCs serao removidos
     */
    public void removeEditorNpcs(final String arenaName) {
        final List<Object> npcs = this.editorNpcs.remove(arenaName);
        if (npcs != null) {
            this.removeNpcs(npcs);
        }
    }

    /**
     * Spawna um unico NPC imediatamente (usado pelo comando add).
     *
     * @param arenaName identificador da arena
     * @param index     indice deste NPC
     * @param npc       NPC da loja a ser spawnado
     * @return o NPC spawnado, ou null se nenhum backend estiver disponivel
     */
    public @org.jetbrains.annotations.Nullable Object spawnSingleNpc(final String arenaName, final int index,
                                                                     final ShopNpc npc) {
        if (npc == null || npc.location() == null || npc.location().getWorld() == null) {
            return null;
        }
        if (!this.isBackendAvailable()) {
            return null;
        }

        final Object created = this.spawnNpc(arenaName, index, npc, "editor");
        if (created != null) {
            this.editorNpcs.computeIfAbsent(arenaName, k -> new ArrayList<>()).add(created);
        }
        return created;
    }

    /**
     * Verifica se um objeto representa um NPC gerenciado pelo BedWars.
     */
    public boolean isManagedNpc(final Object npc) {
        return this.npcHook.isManagedNpc(npc);
    }

    /**
     * Verifica se uma entidade clicada representa um NPC gerenciado pelo BedWars.
     */
    public boolean isManagedEntity(final Entity entity) {
        if (entity == null) {
            return false;
        }
        final String name = this.resolveEntityName(entity);
        return name != null && name.startsWith("bw-shop-");
    }

    /**
     * Remove todos os NPCs (partida e edicao) de todas as arenas.
     */
    public void removeAll() {
        for (final String key : new HashSet<>(this.gameNpcs.keySet())) {
            this.removeGameNpcs(key);
        }
        for (final String key : new HashSet<>(this.editorNpcs.keySet())) {
            this.removeEditorNpcs(key);
        }
    }

    /**
     * Remove todos os NPCs do BedWars que tenham sobrado de sessoes anteriores.
     */
    public void removeAllBedWarsNpcs() {
        if (!this.isFancyNpcsAvailable()) {
            return;
        }

        try {
            final Collection<?> all = this.npcHook.getAllNpcs();
            if (all == null) {
                return;
            }
            for (final Object npc : all) {
                if (this.isManagedNpc(npc)) {
                    this.removeNpcHandle(npc);
                }
            }
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Failed to clean up leftover shop NPCs: " + e.getMessage());
        }
    }

    // ── metodos internos ─────────────────────────────────────────────────

    private boolean isBackendAvailable() {
        return this.isFancyNpcsAvailable();
    }

    private boolean isFancyNpcsAvailable() {
        return this.npcHook.isAvailable();
    }

    private @org.jetbrains.annotations.Nullable List<Object> spawnNpcs(final String arenaName, final List<ShopNpc> npcs,
                                                                       final String context) {
        if (npcs == null || npcs.isEmpty()) {
            return null;
        }
        if (!this.isBackendAvailable()) {
            return null;
        }

        final List<Object> created = new ArrayList<>();

        for (int i = 0; i < npcs.size(); i++) {
            final ShopNpc npc = npcs.get(i);
            if (npc == null || npc.location() == null || npc.location().getWorld() == null) {
                continue;
            }
            final Object createdNpc = this.spawnNpc(arenaName, i, npc, context);
            if (createdNpc != null) {
                created.add(createdNpc);
            }
        }

        return created.isEmpty() ? null : created;
    }

    private @org.jetbrains.annotations.Nullable Object spawnNpc(final String arenaName, final int index,
                                                                final ShopNpc npc, final String context) {
        try {
            if (!this.isFancyNpcsAvailable()) {
                throw new IllegalStateException("FancyNPCs não disponível");
            }
            return this.spawnFancyNpc(arenaName, index, npc, context);
        } catch (final Exception e) {
            final String errorMsg = e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
            this.plugin.getLogger().warning(this.lang.raw("log.shop_npc.spawn_error", String.valueOf(index), arenaName, errorMsg));
            return null;
        }
    }

    private Object spawnFancyNpc(final String arenaName, final int index, final ShopNpc npc,
                                 final String context) throws Exception {
        final String npcName = this.buildNpcName(arenaName, context, index);
        return this.npcHook.createNpc(npcName, UUID.randomUUID(), npc.location(), npc.skin(), npc.displayName());
    }

    private void removeNpcs(final List<Object> npcs) {
        for (final Object npc : npcs) {
            try {
                this.removeNpcHandle(npc);
            } catch (final Exception e) {
                this.plugin.getLogger().warning(this.lang.raw("log.shop_npc.remove_error", e.getMessage()));
            }
        }
    }

    private void removeNpcHandle(final Object npc) throws Exception {
        this.npcHook.removeNpc(npc);
    }

    private @org.jetbrains.annotations.Nullable String resolveNpcName(final Object npc) {
        return this.npcHook.resolveNpcName(npc);
    }

    private @org.jetbrains.annotations.Nullable String resolveEntityName(final Entity entity) {
        if (entity == null) {
            return null;
        }
        final String entityName = entity.getName();
        if (entityName != null && entityName.startsWith("bw-shop-")) {
            return entityName;
        }
        final String customName = entity.getCustomName();
        if (customName != null && customName.startsWith("bw-shop-")) {
            return customName;
        }
        return null;
    }

    private String buildNpcName(final String arenaName, final String context, final int index) {
        return "bw-shop-" + arenaName + "-" + context + "-" + index;
    }

}
