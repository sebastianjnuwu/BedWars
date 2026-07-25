package dev.sebastianjnuwu.bedwars.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.MessageFormat;

/**
 * Gerenciador de mensagens traduzidas (lang).
 * Carrega arquivos da pasta lang/ e fornece mensagens com formatação.
 */
public class LangManager {

    private final JavaPlugin plugin;
    private final String language;
    private YamlConfiguration messages;

    /**
     * Cria o gerenciador de idioma.
     *
     * @param plugin   instância do plugin
     * @param language código do idioma (ex: "pt_BR")
     */
    public LangManager(final JavaPlugin plugin, final String language) {
        this.plugin = plugin;
        this.language = language;
        this.load();
    }

    /**
     * Carrega ou extrai o arquivo de idioma.
     */
    public void load() {
        final File langFolder = new File(this.plugin.getDataFolder(), "lang");
        langFolder.mkdirs();

        final File file = new File(langFolder, this.language + ".yml");
        if (!file.exists()) {
            this.plugin.saveResource("lang/" + this.language + ".yml", false);
        }

        this.messages = YamlConfiguration.loadConfiguration(file);
        this.plugin.getLogger().info("Idioma carregado: " + this.language);
    }

    /**
     * Retorna uma mensagem traduzida sem formatação.
     *
     * @param key  chave no arquivo yml
     * @param args argumentos para formatação
     * @return texto traduzido
     */
    public String raw(final @NotNull String key, final Object... args) {
        String text = this.messages.getString(key, "§c[missing: " + key + "]");
        text = text.replace('&', '§');
        if (args == null || args.length == 0) {
            return text;
        }

        final Object[] actualArgs;
        if (args.length == 1 && args[0] instanceof final Object[] arr) {
            actualArgs = arr;
        } else {
            actualArgs = args;
        }

        for (int i = 0; i < actualArgs.length; i++) {
            final String val = actualArgs[i] != null ? String.valueOf(actualArgs[i]) : "";
            text = text.replace("{" + i + "}", val);
        }
        return text;
    }

    /**
     * Retorna uma mensagem traduzida como Component.
     *
     * @param key  chave no arquivo yml
     * @param args argumentos para formatação
     * @return Component colorido com suporte a códigos legado e adventure
     */
    public Component text(final @NotNull String key, final Object @NotNull ... args) {
        return LegacyComponentSerializer.legacySection().deserialize(this.raw(key, args));
    }

    /**
     * Retorna uma mensagem traduzida como Component com cor personalizada.
     *
     * @param color cor do texto
     * @param key   chave no arquivo yml
     * @param args  argumentos para formatação
     * @return Component colorido
     */
    public Component text(final @NotNull TextColor color, final @NotNull String key, final Object @NotNull ... args) {
        final Component legacyComp = LegacyComponentSerializer.legacySection().deserialize(this.raw(key, args));
        return Component.empty().color(color).append(legacyComp);
    }
}
