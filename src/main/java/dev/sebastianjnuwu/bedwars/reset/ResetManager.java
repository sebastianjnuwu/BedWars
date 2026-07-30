package dev.sebastianjnuwu.bedwars.reset;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.arena.ArenaInstance;
import dev.sebastianjnuwu.bedwars.arena.ArenaState;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;

/**
 * Gerencia o reset de arenas.
 * <p>
 * O reset é feito destruindo a instância atual e criando uma nova limpa do template.
 * Isso evita o uso de loops de blocos que causam lag e queda de TPS.
 * </p>
 */
public class ResetManager {

    private final SlimeManager slimeManager;

    /**
     * Cria um novo gerenciador de reset.
     *
     * @param slimeManager gerenciador de SlimeWorld
     */
    public ResetManager(@NotNull SlimeManager slimeManager) {
        this.slimeManager = slimeManager;
    }

    /**
     * Reinicia uma arena (destrói e recria instância).
     *
     * @param instance instância da arena
     * @return CompletableFuture que completa quando o reset terminar
     */
    public @NotNull CompletableFuture<ArenaInstance> reset(@NotNull ArenaInstance instance) {
        return CompletableFuture.supplyAsync(() -> {
            // Remove instância antiga
            slimeManager.deleteInstance(instance.getInstanceName());

            // Cria nova instância
            final ArenaInstance newInstance = new ArenaInstance(
                    instance.getArena(),
                    instance.getInstanceName(),
                    instance.getTemplateName()
            );

            // Carrega nova instância usando SlimeManager
            final var slimeWorld = slimeManager.loadTemplate(instance.getTemplateName());
            if (slimeWorld == null) {
                throw new IllegalStateException("Template não encontrado: " + instance.getTemplateName());
            }

            newInstance.setWorld(slimeManager.createBukkitWorld(slimeWorld));
            newInstance.setState(ArenaState.READY);

            return newInstance;
        });
    }

    /**
     * Reinicia uma arena pelo nome.
     *
     * @param instanceName nome da instância
     * @return CompletableFuture que completa quando o reset terminar
     */
    public @NotNull CompletableFuture<ArenaInstance> reset(@NotNull String instanceName) {
        // Em produção, você teria um mapa de instâncias por nome
        return CompletableFuture.completedFuture(null);
    }
}
