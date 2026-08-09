package dev.sebastianjnuwu.bedwars.compat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Ponto único de acesso às implementações de compatibilidade.
 * <p>
 * Em tempo de inicialização detecta a versão do servidor
 * ({@code Bukkit.getBukkitVersion()}) e expõe os singletons usados pelo core.
 * A seleção entre a implementação nativa e a legada é feita pelos limiares de
 * API de cada faixa:
 * </p>
 * <ul>
 *   <li>{@link ChatCompat}: nativa em qualquer Paper 1.16.5+ (Adventure nativo);</li>
 *   <li>{@link GolemCompat}: nativa a partir de 1.20.6 (Mob Goal API);</li>
 *   <li>{@link NbtCompat}: nativa em todas as versões do Paper;</li>
 *   <li>{@link PotionCompat}: nativa a partir de 1.20.5 ({@code setBasePotionType});</li>
 *   <li>{@link RegistryCompat}: nativa a partir de 1.19.4 ({@code Registry.ENCHANTMENT});</li>
 *   <li>{@link TeleportCompat}: nativa a partir de 1.20 ({@code teleportAsync}).</li>
 * </ul>
 */
public final class CompatProvider {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^1\\.(\\d+)(?:\\.(\\d+))?");

    private static ChatCompat chat;
    private static GolemCompat golem;
    private static NbtCompat nbt;
    private static PotionCompat potion;
    private static RegistryCompat registry;
    private static TeleportCompat teleport;

    private CompatProvider() {
    }

    /**
     * Inicializa os singletons de compatibilidade baseados na versão atual.
     * Deve ser chamado no {@code onEnable} antes de qualquer uso.
     */
    public static void init() {
        chat = new ChatCompatImpl();
        nbt = new NbtCompatImpl();
        golem = isAtLeast(20, 6) ? new GolemCompatImpl() : new GolemCompatLegacy();
        potion = isAtLeast(20, 5) ? new PotionCompatImpl() : new PotionCompatLegacy();
        registry = isAtLeast(19, 4) ? new RegistryCompatImpl() : new RegistryCompatLegacy();
        teleport = isAtLeast(20, 0) ? new TeleportCompatImpl() : new TeleportCompatLegacy();
    }

    /**
     * @return a versão menor do servidor (ex.: 16, 20, 21) ou 0 se indeterminada
     */
    public static int minorVersion() {
        final String version = Bukkit.getBukkitVersion();
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * Compara a versão do servidor com um limite {@code 1.<minor>.<patch>}.
     *
     * @param minor versão menor mínima (ex.: 20)
     * @param patch versão de patch mínima (ex.: 6)
     * @return {@code true} se a versão do servidor é maior ou igual ao limite
     */
    public static boolean isAtLeast(final int minor, final int patch) {
        final String version = Bukkit.getBukkitVersion();
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) {
            return false;
        }
        final int serverMinor = Integer.parseInt(matcher.group(1));
        final int serverPatch = matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2));
        return serverMinor > minor || (serverMinor == minor && serverPatch >= patch);
    }

    /**
     * @return {@code true} se o servidor é Paper ou um fork com a API do Paper
     */
    public static boolean isPaper() {
        final String version = Bukkit.getVersion();
        return version.contains("Paper") || version.contains("Purpur") || version.contains("Folia");
    }

    public static @NotNull ChatCompat chat() {
        return chat;
    }

    public static @NotNull GolemCompat golem() {
        return golem;
    }

    public static @NotNull NbtCompat nbt() {
        return nbt;
    }

    public static @NotNull PotionCompat potion() {
        return potion;
    }

    public static @NotNull RegistryCompat registry() {
        return registry;
    }

    public static @NotNull TeleportCompat teleport() {
        return teleport;
    }
}
