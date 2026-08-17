package dev.sebastianjnuwu.bedwars.world;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;

/**
 * Ponto único de acesso ao backend de mundos ativo.
 * <p>
 * Em tempo de inicialização detecta a disponibilidade do servidor e expõe o
 * singleton usado pelo core. A seleção é automática: AdvancedSlimePaper quando
 * disponível, caso contrário o Schematic/FAWE (backend de produção).
 * </p>
 */
public final class WorldProviders {

    private static WorldProvider active;

    private WorldProviders() {
    }

    /**
     * Inicializa o singleton com o backend detectado automaticamente.
     * Deve ser chamado no {@code onEnable} antes de qualquer uso.
     *
     * @param plugin plugin BedWars
     * @param lang   gerenciador de internacionalização
     */
    public static void init(final JavaPlugin plugin, final LangManager lang) {
        final SlimeManager slimeManager = new SlimeManager(plugin);
        if (slimeManager.isAvailable()) {
            active = new SlimeWorldProvider(plugin, lang, slimeManager);
            plugin.getLogger().info(lang.raw("startup.world_backend_slime"));
            return;
        }
        active = new SchematicWorldProvider(plugin, lang, new WorldManager(plugin));
        plugin.getLogger().info(lang.raw("startup.world_backend_schematic"));
    }

    /**
     * @return o backend de mundos ativo
     */
    public static @NotNull WorldProvider world() {
        if (active == null) {
            throw new IllegalStateException("WorldProviders não inicializado");
        }
        return active;
    }
}
