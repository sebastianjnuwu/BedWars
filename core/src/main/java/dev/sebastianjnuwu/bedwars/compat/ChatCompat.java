package dev.sebastianjnuwu.bedwars.compat;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * Abstrai o envio de mensagens, títulos e metadados de item via Adventure.
 * <p>
 * Em Paper/Purpur 1.16.5+ a Adventure já está disponível na API; a
 * implementação padrão delega diretamente. Em builds Spigot (sem Adventure),
 * a implementação converte {@link Component} para o formato legado.
 * </p>
 */
public interface ChatCompat {

    void sendMessage(@NotNull CommandSender sender, @NotNull Component message);

    void showTitle(@NotNull Player player, @NotNull Title title);

    void clearTitle(@NotNull Player player);

    void setDisplayName(@NotNull ItemMeta meta, @NotNull Component name);

    void setLore(@NotNull ItemMeta meta, @NotNull List<Component> lore);
}
