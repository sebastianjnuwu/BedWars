package dev.sebastianjnuwu.bedwars.command.admin;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.Arena;

/**
 * Interface funcional para subcomandos de configuração de arena.
 * <p>
 * Diferencia-se de {@link dev.sebastianjnuwu.bedwars.command.SubCommand}
 * por receber também uma instância de {@link Arena} já validada, evitando
 * que cada implementação precise revalidar a arena. É marcada com
 * {@link FunctionalInterface}.
 * </p>
 *
 * @see dev.sebastianjnuwu.bedwars.command.SubCommand
 */
@FunctionalInterface
public interface ArenaSubCommand {
    /**
     * Executa a lógica do subcomando de arena.
     *
     * @param sender o remetente do comando (pode ser console ou jogador)
     * @param arena  a arena alvo já validada (não nula)
     * @param args   os argumentos completos do comando (não nulo, pode ser vazio)
     */
    void execute(CommandSender sender, @NotNull Arena arena, @NotNull String @NotNull [] args);
}
