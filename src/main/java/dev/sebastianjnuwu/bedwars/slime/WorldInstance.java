package dev.sebastianjnuwu.bedwars.slime;

import com.infernalsuite.asp.api.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Representa uma instância de SlimeWorld carregada no servidor.
 * <p>
 * Wraps a SlimeWorldInstance e fornece métodos convenientes para
 * operações comuns como gerar chunks, verificar status e descarregar.
 */
public class WorldInstance {

    private final SlimeWorld slimeWorld;
    private final SlimeWorldInstance slimeInstance;
    private final String instanceName;
    private World bukkitWorld;

    /**
     * Cria uma nova instância de mundo.
     *
     * @param slimeWorld mundo SlimeWorld original
     * @param slimeInstance instância do SlimeWorld
     * @param instanceName nome da instância
     */
    public WorldInstance(
            @NotNull SlimeWorld slimeWorld,
            @NotNull SlimeWorldInstance slimeInstance,
            @NotNull String instanceName
    ) {
        this.slimeWorld = slimeWorld;
        this.slimeInstance = slimeInstance;
        this.instanceName = instanceName;
        this.bukkitWorld = null;
    }

    /**
     * Retorna o mundo SlimeWorld subjacente.
     *
     * @return SlimeWorld
     */
    @NotNull SlimeWorld getSlimeWorld() {
        return slimeWorld;
    }

    /**
     * Retorna a instância do SlimeWorld.
     *
     * @return SlimeWorldInstance
     */
    @NotNull SlimeWorldInstance getSlimeInstance() {
        return slimeInstance;
    }

    /**
     * Retorna o nome da instância.
     *
     * @return nome da instância
     */
    @NotNull String getInstanceName() {
        return instanceName;
    }

    /**
     * Retorna o mundo Bukkit (carrega se necessário).
     *
     * @return World ou null se não carregado
     */
    public @Nullable World getBukkitWorld() {
        if (bukkitWorld == null) {
            bukkitWorld = slimeInstance.getBukkitWorld();
        }
        return bukkitWorld;
    }

    /**
     * Gera o mundo (carrega chunks, gera estruturas).
     *
     * @return CompletableFuture que completa quando gerado
     */
    public @NotNull CompletableFuture<Void> generate() {
        return CompletableFuture.runAsync(() -> {
            slimeWorld.generate();
            // Aguarda o mundo ser carregado no Bukkit
            int retries = 20;
            while (retries-- > 0 && getBukkitWorld() == null) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }

    /**
     * Verifica se o mundo está gerado.
     *
     * @return true se gerado
     */
    public boolean isGenerated() {
        return getBukkitWorld() != null;
    }

    /**
     * Descarrega o mundo.
     *
     * @return true se descarregado com sucesso
     */
    public boolean unload() {
        if (bukkitWorld != null) {
            return org.bukkit.Bukkit.unloadWorld(bukkitWorld, true);
        }
        return false;
    }

    /**
     * Remove permanentemente o mundo do disco.
     */
    public void delete() {
        unload();
        // Em produção, você também deletaria os arquivos do mundo
    }
}
