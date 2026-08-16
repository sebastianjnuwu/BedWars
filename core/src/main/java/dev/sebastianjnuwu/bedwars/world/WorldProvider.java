package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Contrato do backend responsável pelo ciclo de vida dos mundos de partida.
 * <p>
 * Isola a construção de mundos (criar, colar, finalizar, remover) e a
 * persistência de templates do restante do plugin. Duas implementações
 * coexistem de forma independente:
 * </p>
 * <ul>
 *   <li>{@link SchematicWorldProvider}: backend ativo, baseado em
 *       WorldCreator + VoidGenerator + Schematic/FAWE (colagem rápida);</li>
 *   <li>{@link SlimeWorldProvider}: backend ASP (AdvancedSlimePaper), que
 *       clona templates via {@link dev.sebastianjnuwu.bedwars.slime.SlimeManager}.</li>
 * </ul>
 * A implementação ativa é escolhida automaticamente por {@link WorldProviders}
 * com base na disponibilidade do servidor.
 */
public interface WorldProvider {

    /**
     * Identificador do backend.
     *
     * @return nome curto (ex.: "schematic", "slime")
     */
    String id();

    /**
     * Verifica se o backend está disponível no servidor em execução.
     *
     * @return {@code true} se pode ser usado
     */
    boolean isAvailable();

    /**
     * Constrói um mundo de partida pronto a partir do mapa/template da arena.
     * <p>
     * Para o backend schematic, o {@code mapFile} é obrigatório e o mundo é
     * criado vazio (void), colado e finalizado. Para o backend slime, o mundo é
     * clonado do template e o {@code mapFile} é ignorado.
     * </p>
     *
     * @param name      nome da arena (template do mapa)
     * @param worldName nome do mundo de partida a construir
     * @param mapFile   arquivo do mapa (opcional conforme o backend)
     * @param arena     arena com posições de paste/configurações
     * @param errorKey  chave de lang usada no log de falha
     * @return o mundo pronto, ou {@code null} se não foi possível
     */
    @Nullable World buildWorld(String name, String worldName, @Nullable File mapFile, Arena arena, String errorKey);

    /**
     * Constrói um mundo de partida de forma assíncrona, invocando o callback na
     * main thread quando o mundo estiver pronto (ou {@code null} em falha).
     *
     * @param name      nome da arena (template do mapa)
     * @param worldName nome do mundo de partida a construir
     * @param mapFile   arquivo do mapa (opcional conforme o backend)
     * @param arena     arena com posições de paste/configurações
     * @param errorKey  chave de lang usada no log de falha
     * @param callback  consumidor chamado na main thread com o mundo pronto
     */
    void buildWorldAsync(String name, String worldName, @Nullable File mapFile, Arena arena, String errorKey,
                         Consumer<@Nullable World> callback);

    /**
     * Remove um mundo de partida (unload + exclusão do disco).
     *
     * @param worldName nome do mundo
     * @return {@code true} se removido com sucesso
     */
    boolean deleteWorld(String worldName);

    /**
     * Aplica as configurações de mundo da arena (dificuldade, hora, clima e
     * game rules) ao mundo informado.
     *
     * @param world mundo de partida
     * @param arena arena com as configurações
     */
    void applyWorldSettings(World world, Arena arena);

    /**
     * Verifica se um template existe.
     *
     * @param name nome do template
     * @return {@code true} se existe
     */
    boolean templateExists(String name);

    /**
     * Salva um mundo como template.
     *
     * @param name  nome do template
     * @param world mundo de origem
     * @throws IOException se houver erro de I/O
     */
    void saveTemplate(String name, World world) throws IOException;

    /**
     * Retorna o diretório de um template.
     *
     * @param name nome do template
     * @return diretório, ou {@code null} se não existir
     */
    @Nullable File getTemplateFolder(String name);
}