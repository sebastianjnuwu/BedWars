package dev.sebastianjnuwu.bedwars.world;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;

import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.slime.SlimeManager;

/**
 * Backend de mundos baseado em AdvancedSlimePaper (sistema paralelo).
 * <p>
 * Reutiliza o fluxo de construção do {@link SchematicWorldProvider}, mas cria os
 * mundos de partida como mundos Slime vazios gerenciados pelo
 * {@link SlimeManager}, colando o schematic da arena sobre eles. Se a criação do
 * mundo Slime falhar, cai no mundo void padrão (WorldCreator) para não impedir a
 * partida. As configurações da arena são aplicadas pelo fluxo herdado.
 * </p>
 */
public class SlimeWorldProvider extends SchematicWorldProvider {

    private final SlimeManager slimeManager;

    public SlimeWorldProvider(final JavaPlugin plugin, final LangManager lang, final SlimeManager slimeManager) {
        super(plugin, lang, new WorldManager(plugin));
        this.slimeManager = slimeManager;
    }

    @Override
    public String id() {
        return "slime";
    }

    @Override
    public boolean isAvailable() {
        return this.slimeManager.isAvailable();
    }

    @Override
    protected @Nullable World createOrLoadWorld(final String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        final World slimeWorld = this.createEmptySlimeWorld(worldName);
        return slimeWorld != null ? slimeWorld : super.createOrLoadWorld(worldName);
    }

    @Override
    public boolean deleteWorld(final String worldName) {
        this.slimeManager.deleteInstance(worldName);
        return true;
    }

    @Override
    public boolean templateExists(final String name) {
        return this.slimeManager.getTemplateFolder(name).isDirectory();
    }

    @Override
    public void saveTemplate(final String name, final World world) throws IOException {
        this.slimeManager.saveTemplate(name, world).join();
    }

    @Override
    public @Nullable File getTemplateFolder(final String name) {
        final File folder = this.slimeManager.getTemplateFolder(name);
        return folder.isDirectory() ? folder : null;
    }

    private @Nullable World createEmptySlimeWorld(final String worldName) {
        try {
            final SlimeWorld slimeWorld = this.slimeManager.createVoidSlimeWorld(worldName);
            if (slimeWorld == null) {
                return null;
            }
            final SlimeWorldInstance instance = AdvancedSlimePaperAPI.instance().loadWorld(slimeWorld, false);
            return instance == null ? null : instance.getBukkitWorld();
        } catch (final Exception e) {
            return null;
        }
    }
}
