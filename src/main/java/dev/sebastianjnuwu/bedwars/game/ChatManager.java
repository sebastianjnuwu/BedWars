package dev.sebastianjnuwu.bedwars.game;

import java.util.Collection;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * Responsável pelo envio de mensagens, títulos e sons da partida.
 * <p>
 * Todos os métodos deste gerenciador alcançam apenas os jogadores da partida
 * ({@link Game#getPlayers()} e espectadores), nunca jogadores de fora ou de
 * outras partidas do servidor. O uso de {@code Bukkit.getOnlinePlayers()} deve
 * ser evitado fora daqui.
 * </p>
 */
public final class ChatManager {

    private final Game game;

    /**
     * Cria o gerenciador de chat para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public ChatManager(final @NotNull Game game) {
        this.game = game;
    }

    /**
     * Envia uma mensagem apenas para os jogadores em partida (sem espectadores).
     *
     * @param message mensagem a ser enviada
     */
    public void sendToPlayers(final @NotNull Component message) {
        for (final Player player : this.game.getPlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * Envia uma mensagem para todos os presentes na partida (jogadores e espectadores).
     *
     * @param message mensagem a ser enviada
     */
    public void broadcast(final @NotNull Component message) {
        this.sendToPlayers(message);
        for (final Player spectator : this.game.getSpectatorPlayers()) {
            spectator.sendMessage(message);
        }
    }

    /**
     * Exibe um título para todos os presentes na partida (jogadores e espectadores).
     *
     * @param title título a ser exibido
     */
    public void showTitle(final @NotNull Title title) {
        for (final Player player : this.allPresent()) {
            player.showTitle(title);
        }
    }

    /**
     * Limpa o título de todos os presentes na partida.
     */
    public void clearTitle() {
        for (final Player player : this.allPresent()) {
            player.clearTitle();
        }
    }

    /**
     * Toca um som para todos os presentes na partida.
     *
     * @param sound  som a ser tocado
     * @param volume volume (padrão {@code 1.0})
     * @param pitch  tom do som (padrão {@code 1.0})
     */
    public void playSound(final @NotNull Sound sound, final float volume, final float pitch) {
        for (final Player player : this.allPresent()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
    }

    /**
     * Envia uma mensagem e toca um som para todos os presentes na partida.
     * <p>
     * Conveniência usada pelos eventos do jogo (cama destruída, eliminação,
     * vitória, etc.) que precisam anunciar algo com destaque sonoro.
     * </p>
     *
     * @param message mensagem a ser enviada
     * @param sound   som a ser tocado
     * @param volume  volume do som
     * @param pitch   tom do som
     */
    public void broadcastWithSound(final @NotNull Component message, final @NotNull Sound sound,
                                   final float volume, final float pitch) {
        this.broadcast(message);
        this.playSound(sound, volume, pitch);
    }

    /**
     * Retorna todos os jogadores presentes na partida (jogadores e espectadores).
     *
     * @return lista mutável de jogadores presentes
     */
    public @NotNull Collection<Player> getPresentPlayers() {
        return this.allPresent();
    }

    private Collection<Player> allPresent() {
        final Collection<Player> present = this.game.getPlayers();
        present.addAll(this.game.getSpectatorPlayers());
        return present;
    }
}
