package dev.sebastianjnuwu.bedwars.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{(\\d+)\\}");

    /**
     * Retorna uma mensagem traduzida sem formatação.
     *
     * @param key  chave no arquivo yml
     * @param args argumentos para formatação
     * @return texto traduzido
     */
    public String raw(final @NotNull String key, final Object... args) {
        String text = this.messages.getString(key, "§c[missing: " + key + "]");
        if (text == null) {
            return "§c[missing: " + key + "]";
        }

        if (args != null && args.length > 0) {
            text = this.replacePlaceholders(text, args);
        }

        text = text.replace('&', '§');
        return text;
    }

    private String replacePlaceholders(String text, final Object... args) {
        final Object[] actualArgs;
        if (args.length == 1 && args[0] instanceof final Object[] arr) {
            actualArgs = arr;
        } else {
            actualArgs = args;
        }

        final Matcher matcher = VAR_PATTERN.matcher(text);
        if (!matcher.find()) {
            return text;
        }
        matcher.reset();

        final StringBuilder sb = new StringBuilder(text.length());
        while (matcher.find()) {
            final int idx = Integer.parseInt(matcher.group(1));
            final String val;
            if (idx < actualArgs.length && actualArgs[idx] != null) {
                val = String.valueOf(actualArgs[idx]);
            } else {
                this.plugin.getLogger().log(Level.WARNING,
                        "Placeholder {" + idx + "} não encontrado em args para a chave \"" + text + "\"");
                val = matcher.group(0);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
