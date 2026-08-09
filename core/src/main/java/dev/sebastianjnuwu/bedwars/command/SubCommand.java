package dev.sebastianjnuwu.bedwars.command;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Interface funcional para subcomandos do BedWars.
 * <p>
 * Define um contrato único {@link #execute(CommandSender, String[])} que
 * todas as implementações de subcomando (como {@code LifecycleRouter},
 * {@code SetLobbyCommand}, etc.) devem seguir. É marcada com
 * {@link FunctionalInterface}, permitindo o uso de expressões lambda
 * quando desejado.
 * </p>
 *
 * @see ArenaSubCommand
 */
@FunctionalInterface
public interface SubCommand {
    /**
     * Executa a lógica do subcomando.
     *
     * @param sender o remetente do comando (pode ser console ou jogador)
     * @param args   os argumentos restantes após o subcomando (não nulo,
     *               pode ser vazio)
     */
    void execute(CommandSender sender, @NotNull String @NotNull [] args);
}
