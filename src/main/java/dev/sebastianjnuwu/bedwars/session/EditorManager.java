package dev.sebastianjnuwu.bedwars.session;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia sessões de edição de arenas.
 * Garante que apenas um jogador por vez edite uma arena.
 */
public class EditorManager {

    private final Map<String, UUID> arenaEditors;
    private final Map<UUID, String> playerArenas;

    /**
     * Cria o gerenciador de sessões.
     */
    public EditorManager() {
        this.arenaEditors = new HashMap<>();
        this.playerArenas = new HashMap<>();
    }

    /**
     * Inicia uma sessão de edição.
     *
     * @param player  jogador
     * @param arenaName nome da arena
     * @return true se a sessão foi iniciada, false se já está ocupada
     */
    public boolean startSession(final Player player, final String arenaName) {
        final UUID owner = this.arenaEditors.get(arenaName);
        if (owner != null && !owner.equals(player.getUniqueId())) {
            return false;
        }
        this.arenaEditors.put(arenaName, player.getUniqueId());
        this.playerArenas.put(player.getUniqueId(), arenaName);
        return true;
    }

    /**
     * Finaliza a sessão de edição de um jogador.
     *
     * @param player jogador
     */
    public void endSession(final Player player) {
        final String arenaName = this.playerArenas.remove(player.getUniqueId());
        if (arenaName != null) {
            this.arenaEditors.remove(arenaName);
        }
    }

    /**
     * Finaliza a sessão de uma arena específica.
     *
     * @param arenaName nome da arena
     */
    public void endSession(final String arenaName) {
        final UUID owner = this.arenaEditors.remove(arenaName);
        if (owner != null) {
            this.playerArenas.remove(owner);
        }
    }

    /**
     * Verifica se um jogador está editando uma arena específica.
     *
     * @param player    jogador
     * @param arenaName nome da arena
     * @return true se é o editor atual
     */
    public boolean isEditing(final Player player, final String arenaName) {
        final UUID owner = this.arenaEditors.get(arenaName);
        return owner != null && owner.equals(player.getUniqueId());
    }

    /**
     * Retorna o nome do jogador que está editando uma arena.
     *
     * @param arenaName nome da arena
     * @return nome do editor ou null
     */
    public @Nullable String getEditorName(final String arenaName) {
        final UUID owner = this.arenaEditors.get(arenaName);
        if (owner == null) {
            return null;
        }
        final Player player = org.bukkit.Bukkit.getPlayer(owner);
        return player != null ? player.getName() : "desconhecido";
    }

    /**
     * Verifica se uma arena está sendo editada por alguém.
     *
     * @param arenaName nome da arena
     * @return true se está ocupada
     */
    public boolean isBeingEdited(final String arenaName) {
        return this.arenaEditors.containsKey(arenaName);
    }
}
