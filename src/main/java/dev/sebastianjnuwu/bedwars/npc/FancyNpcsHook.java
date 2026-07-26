package dev.sebastianjnuwu.bedwars.npc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FancyNpcsHook {

    private static final String PLUGIN_NAME = "FancyNpcs";
    private static boolean available = false;
    private static Object npcManager;
    private static Method registerMethod;
    private static Method removeMethod;
    private static Method getNpcMethod;
    private static Method removeForAllMethod;
    private static Method createMethod;
    private static Method spawnForAllMethod;
    private static Method setSaveToFileMethod;
    private static Method getDataMethod;
    private static Method getNameMethod;
    private static Method getEntityIdMethod;
    private static final Map<String, Object> npcs = new HashMap<>();

    public static boolean isAvailable() {
        return available;
    }

    public static void init() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
        if (plugin == null) return;
        try {
            final ClassLoader loader = plugin.getClass().getClassLoader();
            final Class<?> pluginClass = loader.loadClass("de.oliver.FancyNpcs.api.FancyNpcsPlugin");
            final Method getMethod = pluginClass.getMethod("get");
            final Object instance = getMethod.invoke(null);
            final Method getNpcManagerMethod = pluginClass.getMethod("getNpcManager");
            npcManager = getNpcManagerMethod.invoke(instance);
            final Class<?> managerClass = npcManager.getClass();
            registerMethod = managerClass.getMethod("registerNpc", Object.class);
            removeMethod = managerClass.getMethod("removeNpc", Object.class);
            getNpcMethod = managerClass.getMethod("getNpc", String.class);

            final Class<?> npcClass = loader.loadClass("de.oliver.FancyNpcs.api.Npc");
            removeForAllMethod = npcClass.getMethod("removeForAll");
            createMethod = npcClass.getMethod("create");
            spawnForAllMethod = npcClass.getMethod("spawnForAll");
            setSaveToFileMethod = npcClass.getMethod("setSaveToFile", boolean.class);
            getDataMethod = npcClass.getMethod("getData");
            getEntityIdMethod = npcClass.getMethod("getEntityId");

            final Class<?> dataClass = loader.loadClass("de.oliver.FancyNpcs.api.data.NpcData");
            getNameMethod = dataClass.getMethod("getName");

            available = true;
        } catch (final Exception ignored) {
        }
    }

    public static @Nullable Object createNpc(final String id, final Location location, final @Nullable String skin, final @Nullable String displayName) {
        if (!available) return null;
        try {
            final Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
            final ClassLoader loader = plugin.getClass().getClassLoader();
            final Class<?> dataClass = loader.loadClass("de.oliver.FancyNpcs.api.data.NpcData");
            final Object data = dataClass.getConstructor(String.class, UUID.class, Location.class).newInstance(id, UUID.randomUUID(), location);
            if (skin != null) dataClass.getMethod("setSkin", String.class).invoke(data, skin);
            if (displayName != null) dataClass.getMethod("setDisplayName", String.class).invoke(data, displayName);

            final Class<?> pluginClass = loader.loadClass("de.oliver.FancyNpcs.api.FancyNpcsPlugin");
            final Object instance = pluginClass.getMethod("get").invoke(null);
            final Object adapter = pluginClass.getMethod("getNpcAdapter").invoke(instance);
            final Object npc = adapter.getClass().getMethod("apply", Object.class).invoke(adapter, data);

            setSaveToFileMethod.invoke(npc, false);
            registerMethod.invoke(npcManager, npc);
            createMethod.invoke(npc);
            spawnForAllMethod.invoke(npc);
            npcs.put(id, npc);
            return npc;
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static void removeNpc(final String id) {
        if (!available) return;
        try {
            final Object npc = npcs.remove(id);
            if (npc != null) {
                removeForAllMethod.invoke(npc);
                removeMethod.invoke(npcManager, npc);
            }
        } catch (final Exception ignored) {
        }
    }

    public static @Nullable Object getNpc(final int entityId) {
        if (!available) return null;
        try {
            for (final Object npc : npcs.values()) {
                if ((int) getEntityIdMethod.invoke(npc) == entityId) {
                    return npc;
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    public static @Nullable String getNpcName(final Object npc) {
        if (!available) return null;
        try {
            final Object data = getDataMethod.invoke(npc);
            return (String) getNameMethod.invoke(data);
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static int getNpcEntityId(final Object npc) {
        if (!available) return -1;
        try {
            return (int) getEntityIdMethod.invoke(npc);
        } catch (final Exception ignored) {
            return -1;
        }
    }
}
