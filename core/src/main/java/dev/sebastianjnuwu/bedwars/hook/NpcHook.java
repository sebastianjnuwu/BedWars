package dev.sebastianjnuwu.bedwars.hook;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Contrato simples para integrações com backends de NPC.
 */
public interface NpcHook {

    /**
     * Verifica se o backend está disponível no servidor.
     */
    boolean isAvailable();

    /**
     * Cria um NPC no backend ativo.
     */
    Object createNpc(String npcName, UUID creator, Location location, String skin, String displayName) throws Exception;

    /**
     * Remove um NPC do backend ativo.
     */
    void removeNpc(Object npc) throws Exception;

    /**
     * Lista todos os NPCs registrados pelo backend.
     */
    Collection<?> getAllNpcs() throws Exception;

    /**
     * Verifica se um objeto representa um NPC gerenciado pelo BedWars.
     */
    boolean isManagedNpc(Object npc);

    /**
     * Verifica se uma entidade do mundo representa um NPC gerenciado pelo BedWars.
     */
    default boolean isManagedEntity(Entity entity) {
        return false;
    }

    /**
     * Resolve o nome de um NPC gerenciado pelo backend.
     */
    String resolveNpcName(Object npc);
}
