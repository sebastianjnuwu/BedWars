package dev.sebastianjnuwu.bedwars.world;

import java.io.IOException;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gerencia templates e instâncias de mundos usando SlimeWorld.
 */
public interface SlimeWorldManager {

    /**
     * Salva um mundo como template.
     *
     * @param name nome do template
     * @param world mundo original
     * @throws IOException se houver erro
     */
    void saveTemplate(@NotNull String name, @NotNull World world) throws IOException;

    /**
     * Verifica se um template existe.
     *
     * @param name nome do template
     * @return true se existe
     */
    boolean templateExists(@NotNull String name);

    /**
     * Cria uma nova instância a partir de um template.
     *
     * @param templateName nome do template
     * @param instanceName nome da instância
     * @return mundo instanciado ou null
     * @throws IOException se houver erro
     */
    @Nullable World createInstance(@NotNull String templateName, @NotNull String instanceName) throws IOException;

    /**
     * Carrega uma instância existente.
     *
     * @param name nome da instância
     * @return mundo carregado ou null
     */
    @Nullable World loadInstance(@NotNull String name);

    /**
     * Descarrega uma instância.
     *
     * @param name nome da instância
     * @return true se descarregou
     */
    boolean unloadInstance(@NotNull String name);

    /**
     * Remove uma instância permanentemente.
     *
     * @param name nome da instância
     */
    void deleteInstance(@NotNull String name);

    /**
     * Retorna o diretório de templates.
     *
     * @return diretório de templates
     */
    @NotNull java.io.File getTemplatesFolder();

    /**
     * Retorna o diretório onde instâncias são criadas.
     *
     * @return diretório de instâncias
     */
    @NotNull java.io.File getInstancesFolder();

    /**
     * Verifica se uma instância está carregada.
     *
     * @param name nome da instância
     * @return true se carregada
     */
    boolean isInstanceLoaded(@NotNull String name);
}
