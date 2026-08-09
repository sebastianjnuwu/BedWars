package dev.sebastianjnuwu.bedwars.hook;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.lang.LangManager;

/**
 * Hook dedicado para FancyNPCs.
 */
public class FancyNpcsHook implements NpcHook {

    private final JavaPlugin plugin;

    public FancyNpcsHook(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        return this.isClassAvailable("de.oliver.fancynpcs.api.FancyNpcsPlugin");
    }

    @Override
    public Object createNpc(final String npcName, final UUID creator, final Location location,
                            final String skin, final String displayName) throws Exception {
        if (!this.isAvailable()) {
            throw new IllegalStateException("FancyNPCs não disponível");
        }

        final Class<?> npcDataClass = Class.forName("de.oliver.fancynpcs.api.NpcData");
        final Constructor<?> constructor = npcDataClass.getConstructor(String.class, UUID.class, Location.class);
        final Object npcData = constructor.newInstance(npcName, creator, location);

        try {
            this.invokeMethod(npcData, "setSkin", new Class<?>[]{String.class}, skin != null ? skin : "NPC");
        } catch (final Exception e) {
            final LangManager lang = ((BedWarsPlugin) this.plugin).getLang();
            final String errorMsg = e.getMessage() != null ? e.getMessage() : "Erro desconhecido";
            this.plugin.getLogger().warning(lang.raw("log.shop_npc.skin_fallback", skin != null ? skin : "null", npcName, errorMsg));
        }
        this.invokeMethod(npcData, "setDisplayName", new Class<?>[]{String.class}, displayName != null ? displayName : "<red>Loja</red>");

        final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
        final Object npcAdapter = this.invokeMethod(fancyPlugin, "getNpcAdapter", new Class<?>[0]);
        final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
        final Object npc = this.invokeMethod(npcAdapter, "apply", new Class<?>[]{Object.class}, npcData);

        this.invokeMethod(npc, "setSaveToFile", new Class<?>[]{boolean.class}, false);
        this.invokeMethod(npcManager, "registerNpc", new Class<?>[]{Class.forName("de.oliver.fancynpcs.api.Npc")}, npc);
        this.invokeMethod(npc, "create", new Class<?>[0]);
        this.invokeMethod(npc, "spawnForAll", new Class<?>[0]);
        return npc;
    }

    @Override
    public void removeNpc(final Object npc) throws Exception {
        if (npc == null) {
            return;
        }

        final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
        final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
        this.invokeMethod(npcManager, "removeNpc", new Class<?>[]{Class.forName("de.oliver.fancynpcs.api.Npc")}, npc);
        this.invokeMethod(npc, "removeForAll", new Class<?>[0]);
    }

    @Override
    public Collection<?> getAllNpcs() throws Exception {
        final Object fancyPlugin = this.invokeStaticMethod("de.oliver.fancynpcs.api.FancyNpcsPlugin", "get", new Class<?>[0]);
        final Object npcManager = this.invokeMethod(fancyPlugin, "getNpcManager", new Class<?>[0]);
        return (Collection<?>) this.invokeMethod(npcManager, "getAllNpcs", new Class<?>[0]);
    }

    @Override
    public boolean isManagedNpc(final Object npc) {
        return npc != null && this.resolveNpcName(npc) != null && this.resolveNpcName(npc).startsWith("bw-shop-");
    }

    @Override
    public String resolveNpcName(final Object npc) {
        if (npc == null) {
            return null;
        }
        try {
            final Object data = this.invokeMethod(npc, "getData", new Class<?>[0]);
            return (String) this.invokeMethod(data, "getName", new Class<?>[0]);
        } catch (final Exception e) {
            return null;
        }
    }

    private boolean isClassAvailable(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
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
