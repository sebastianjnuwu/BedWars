package dev.sebastianjnuwu.bedwars.hook;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Hook dedicado para Citizens.
 * <p>
 * Usa reflexão para não exigir o Citizens no classpath de compilação; a
 * integração só é carregada quando o plugin está presente no servidor.
 * </p>
 */
public class CitizensHook implements NpcHook {

    private static final String CITIZENS_API = "net.citizensnpcs.api.CitizensAPI";
    private static final String SKIN_TRAIT = "net.citizensnpcs.trait.SkinTrait";
    private static final String MARKER_KEY = "bw-shop-marker";

    private final JavaPlugin plugin;

    public CitizensHook(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName(CITIZENS_API);
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public Object createNpc(final String npcName, final UUID creator, final Location location,
                            final String skin, final String displayName) throws Exception {
        if (!this.isAvailable()) {
            throw new IllegalStateException("Citizens não disponível");
        }

        final Object registry = this.invokeStaticMethod(CITIZENS_API, "getNPCRegistry", new Class<?>[0]);
        final Object npc = this.invokeMethod(registry, "createNPC",
                new Class<?>[]{EntityType.class, String.class}, EntityType.PLAYER, npcName);

        final Object data = this.invokeMethod(npc, "data", new Class<?>[0]);
        this.invokeMethod(data, "setPersistent", new Class<?>[]{String.class, Object.class}, MARKER_KEY, npcName);

        final String visibleName = displayName != null && !displayName.isBlank()
                ? toLegacy(displayName)
                : "Loja";
        this.invokeMethod(npc, "setName", new Class<?>[]{String.class}, visibleName);

        this.applySkin(npc, skin, npcName);
        this.invokeMethod(npc, "spawn", new Class<?>[]{Location.class}, location);
        return npc;
    }

    @Override
    public void removeNpc(final Object npc) throws Exception {
        if (npc == null) {
            return;
        }
        this.invokeMethod(npc, "destroy", new Class<?>[0]);
    }

    @Override
    public Collection<?> getAllNpcs() throws Exception {
        final Object registry = this.invokeStaticMethod(CITIZENS_API, "getNPCRegistry", new Class<?>[0]);
        final List<Object> result = new ArrayList<>();
        for (final Object npc : (Iterable<?>) registry) {
            result.add(npc);
        }
        return result;
    }

    @Override
    public boolean isManagedNpc(final Object npc) {
        if (npc == null) {
            return false;
        }
        try {
            final Object data = this.invokeMethod(npc, "data", new Class<?>[0]);
            return (boolean) this.invokeMethod(data, "has", new Class<?>[]{String.class}, MARKER_KEY);
        } catch (final Exception e) {
            return false;
        }
    }

    @Override
    public String resolveNpcName(final Object npc) {
        if (npc == null) {
            return null;
        }
        try {
            final Object data = this.invokeMethod(npc, "data", new Class<?>[0]);
            return (String) this.invokeMethod(data, "get", new Class<?>[]{String.class}, MARKER_KEY);
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    public boolean isManagedEntity(final Entity entity) {
        if (entity == null) {
            return false;
        }
        try {
            final Object registry = this.invokeStaticMethod(CITIZENS_API, "getNPCRegistry", new Class<?>[0]);
            final Object npc = this.invokeMethod(registry, "getNPC", new Class<?>[]{Entity.class}, entity);
            return npc != null && this.isManagedNpc(npc);
        } catch (final Exception e) {
            return false;
        }
    }

    private void applySkin(final Object npc, final String skin, final String npcName) {
        if (skin == null || skin.isBlank() || "NPC".equalsIgnoreCase(skin)) {
            return;
        }
        try {
            final Object skinTraitClass = Class.forName(SKIN_TRAIT);
            final Object trait = this.invokeMethod(npc, "getOrAddTrait", new Class<?>[]{Class.class}, skinTraitClass);
            this.invokeMethod(trait, "setSkinName", new Class<?>[]{String.class}, skin);
        } catch (final Exception e) {
            this.plugin.getLogger().warning("Falha ao definir skin '" + skin + "' para NPC '" + npcName + "': "
                    + (e.getMessage() != null ? e.getMessage() : "Erro desconhecido"));
        }
    }

    private static String toLegacy(final String message) {
        try {
            return LegacyComponentSerializer.legacySection()
                    .serialize(MiniMessage.miniMessage().deserialize(message));
        } catch (final Exception e) {
            return message;
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
            for (final Class<?> iface : current.getInterfaces()) {
                final Method method = this.findMethod(iface, methodName, parameterTypes);
                if (method != null) {
                    return method;
                }
            }
            current = current.getSuperclass();
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
