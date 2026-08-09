package dev.sebastianjnuwu.bedwars.arena;

import java.util.UUID;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Representa uma instância runtime de uma arena.
 * Cada partida tem sua própria instância isolada criada a partir de um template Slime.
 */
public class ArenaInstance {

    private final UUID instanceId;
    private final Arena arena;
    private final String instanceName;
    private final String templateName;
    private volatile World world;
    private volatile ArenaState state;

    /**
     * Cria uma nova instância de arena.
     *
     * @param arena arena estática
     * @param instanceName nome único da instância
     * @param templateName nome do template original
     */
    public ArenaInstance(
            @NotNull Arena arena,
            @NotNull String instanceName,
            @NotNull String templateName
    ) {
        this.instanceId = UUID.randomUUID();
        this.arena = arena;
        this.instanceName = instanceName;
        this.templateName = templateName;
        this.state = ArenaState.OFFLINE;
        this.world = null;
    }

    /**
     * Retorna a arena estática associada.
     *
     * @return arena
     */
    public @NotNull Arena getArena() {
        return arena;
    }

    /**
     * Retorna o mundo da instância.
     *
     * @return mundo ou null
     */
    public @Nullable World getWorld() {
        return world;
    }

    /**
     * Define o mundo da instância.
     *
     * @param world mundo
     */
    public void setWorld(@Nullable World world) {
        this.world = world;
    }

    /**
     * Retorna o nome da instância.
     *
     * @return nome da instância
     */
    public @NotNull String getInstanceName() {
        return instanceName;
    }

    /**
     * Retorna o nome do template.
     *
     * @return nome do template
     */
    public @NotNull String getTemplateName() {
        return templateName;
    }

    /**
     * Retorna o estado atual.
     *
     * @return estado
     */
    public @NotNull ArenaState getState() {
        return state;
    }

    /**
     * Define o estado atual.
     *
     * @param state estado
     */
    public void setState(@NotNull ArenaState state) {
        this.state = state;
    }

    /**
     * Retorna o UUID da instância.
     *
     * @return UUID
     */
    @NotNull UUID getInstanceId() {
        return instanceId;
    }

    /**
     * Verifica se o mundo está carregado.
     *
     * @return true se carregado
     */
    boolean isLoaded() {
        return world != null && world.getName() != null && org.bukkit.Bukkit.getWorld(world.getName()) != null;
    }
}
