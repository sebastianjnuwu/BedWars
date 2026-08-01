package dev.sebastianjnuwu.bedwars.shop;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Gerencia o ciclo de vida dos NPCs da loja para as arenas do BedWars.
 * <p>
 * Os NPCs são criados via FancyNPCs quando o backend estiver disponível.
 */
public class ShopNpcManager {

    private final JavaPlugin plugin;
    private final LangManager lang;
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
        this.removeEditorNpcs(arenaName);
        final List<Object> npcs = this.spawnNpcs(arenaName, locations, skin, "jogo");
        if (npcs != null) {
            this.gameNpcs.put(arenaName, npcs);
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
     * @param locations localizacoes dos NPCs
     * @param skin      nome da skin ou null para padrao
     */
    public void spawnEditorNpcs(final String arenaName, final List<Location> locations, final String skin) {
        this.removeEditorNpcs(arenaName);
        final List<Object> npcs = this.spawnNpcs(arenaName, locations, skin, "editor");
        if (npcs != null) {
            this.editorNpcs.put(arenaName, npcs);
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
     * @param location  local onde spawnar
     * @param skin      nome da skin ou null para padrao
     * @return o NPC spawnado, ou null se nenhum backend estiver disponivel
     */
    public @org.jetbrains.annotations.Nullable Object spawnSingleNpc(final String arenaName, final int index,
                                                                     final Location location, final String skin) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        if (!this.isBackendAvailable()) {
            return null;
        }

        final Object npc = this.spawnNpc(arenaName, index, location, skin, "editor");
        if (npc != null) {
            this.editorNpcs.computeIfAbsent(arenaName, k -> new ArrayList<>()).add(npc);
        }
        return npc;
    }

    /**
     * Verifica se um objeto representa um NPC gerenciado pelo BedWars.
     */
    public boolean isManagedNpc(final Object npc) {
        return npc != null && this.resolveNpcName(npc) != null && this.resolveNpcName(npc).startsWith("bw-shop-");
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
            final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
            final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
            final Collection<?> all = (Collection<?>) this.invokeMethod(npcManager, "getAllNpcs", new Class<?>[0]);
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
        return this.isClassAvailable("de.oliver.fancynpcs.api.FancyNpcsPlugin");
    }

    private boolean isClassAvailable(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private @org.jetbrains.annotations.Nullable List<Object> spawnNpcs(final String arenaName, final List<Location> locations,
                                                                       final String skin, final String context) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        if (!this.isBackendAvailable()) {
            return null;
        }

        final List<Object> npcs = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            final Location loc = locations.get(i);
            if (loc == null || loc.getWorld() == null) {
                continue;
            }
            final Object npc = this.spawnNpc(arenaName, i, loc, skin, context);
            if (npc != null) {
                npcs.add(npc);
            }
        }

        return npcs.isEmpty() ? null : npcs;
    }

    private @org.jetbrains.annotations.Nullable Object spawnNpc(final String arenaName, final int index,
                                                               final Location loc, final String skin, final String context) {
        try {
            if (!this.isFancyNpcsAvailable()) {
                throw new IllegalStateException("FancyNPCs não disponível");
            }
            return this.spawnFancyNpc(arenaName, index, loc, skin, context);
        } catch (final Exception e) {
            this.plugin.getLogger().warning(this.lang.raw("log.shop_npc.spawn_error", String.valueOf(index), arenaName, e.getMessage()));
            return null;
        }
    }

    private Object spawnFancyNpc(final String arenaName, final int index, final Location loc,
                                 final String skin, final String context) throws Exception {
        final String npcName = this.buildNpcName(arenaName, context, index);
        final Class<?> npcDataClass = Class.forName("de.oliver.fancynpcs.api.NpcData");
        final Constructor<?> constructor = npcDataClass.getConstructor(String.class, UUID.class, Location.class);
        final Object npcData = constructor.newInstance(npcName, UUID.randomUUID(), loc);
        this.invokeMethod(npcData, "setSkin", new Class<?>[]{String.class}, skin != null ? skin : "NPC");
        this.invokeMethod(npcData, "setDisplayName", new Class<?>[]{String.class}, "<red>Loja</red>");

        final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
        final Object npcAdapter = this.invokeMethod(fancyPlugin, "getNpcAdapter", new Class<?>[0]);
        final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
        final Object npc = this.invokeMethod(npcAdapter, "apply", new Class<?>[]{npcDataClass}, npcData);
        this.invokeMethod(npc, "setSaveToFile", new Class<?>[]{boolean.class}, false);
        this.invokeMethod(npcManager, "registerNpc", new Class<?>[]{Class.forName("de.oliver.fancynpcs.api.Npc")}, npc);
        this.invokeMethod(npc, "create", new Class<?>[0]);
        this.invokeMethod(npc, "spawnForAll", new Class<?>[0]);
        return npc;
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
        if (npc == null) {
            return;
        }
        if (this.isFancyNpcHandle(npc)) {
            final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
            final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
            this.invokeMethod(npcManager, "removeNpc", new Class<?>[]{Class.forName("de.oliver.fancynpcs.api.Npc")}, npc);
            this.invokeMethod(npc, "removeForAll", new Class<?>[0]);
            return;
        }
        if (this.isFancyNpcHandle(npc)) {
            this.invokeMethod(npc, "destroy", new Class<?>[0]);
        }
    }

    private boolean isFancyNpcHandle(final Object npc) {
        return npc != null && npc.getClass().getName().startsWith("de.oliver.fancynpcs");
    }

    private @org.jetbrains.annotations.Nullable String resolveNpcName(final Object npc) {
        if (npc == null) {
            return null;
        }
        if (this.isFancyNpcHandle(npc)) {
            try {
                final Object data = this.invokeMethod(npc, "getData", new Class<?>[0]);
                return (String) this.invokeMethod(data, "getName", new Class<?>[0]);
            } catch (final Exception e) {
                return null;
            }
        }
        return null;
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

    private Method findMethod(final Class<?> type, final String methodName, final Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (final NoSuchMethodException ignored) {
                // continue searching
            }
            current = current.getSuperclass();
        }

        for (final Class<?> iface : type.getInterfaces()) {
            final Method method = this.findMethod(iface, methodName, parameterTypes);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private Object invokeMethod(final Object target, final String methodName, final Class<?>[] parameterTypes,
                                final Object... args) throws Exception {
        final Method method = this.findMethod(target.getClass(), methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object invokeStaticMethod(final String className, final String methodName, final Class<?>[] parameterTypes,
                                      final Object... args) throws Exception {
        final Class<?> clazz = Class.forName(className);
        final Method method = this.findMethod(clazz, methodName, parameterTypes);
        if (method == null) {
            throw new NoSuchMethodException(methodName);
        }
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}
