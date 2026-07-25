package dev.sebastianjnuwu.bedwars.command;

import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;

import java.io.File;

/**
 * Classe base abstrata para todos os comandos do BedWars.
 * <p>
 * Fornece acesso centralizado aos gerenciadores do plugin ({@link ArenaManager},
 * {@link EditorManager}, {@link ConfigManager}, {@link GameManager} e
 * {@link LangManager}) e ao diretório de mapas. Todas as subclasses de comando
 * concretas devem estender esta classe para reutilizar essas dependências.
 * </p><p>
 * <b>Uso:</b> As subclasses chamam o construtor {@code super(...)} para receber
 * as instâncias dos gerenciadores.
 * </p>
 *
 * @see ArenaManager
 * @see EditorManager
 * @see ConfigManager
 * @see GameManager
 * @see LangManager
 */
public abstract class BaseCommand {

    protected final ArenaManager arenaManager;
    protected final EditorManager editorManager;
    protected final ConfigManager configManager;
    protected final GameManager gameManager;
    protected final LangManager lang;
    protected final File mapsFolder;

    /**
     * Construtor protegido que inicializa as dependências do comando.
     *
     * @param arenaManager  gerenciador de arenas (não nulo)
     * @param editorManager gerenciador de edição de sessão (não nulo)
     * @param configManager gerenciador de configuração do plugin (não nulo)
     * @param gameManager   gerenciador da lógica do jogo (não nulo)
     * @param lang          gerenciador de internacionalização/mensagens (não nulo)
     * @param mapsFolder    diretório onde os mapas das arenas estão armazenados (não nulo)
     */
    protected BaseCommand(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final ConfigManager configManager,
            final GameManager gameManager,
            final LangManager lang,
            final File mapsFolder
    ) {
        this.arenaManager = arenaManager;
        this.editorManager = editorManager;
        this.configManager = configManager;
        this.gameManager = gameManager;
        this.lang = lang;
        this.mapsFolder = mapsFolder;
    }
}
