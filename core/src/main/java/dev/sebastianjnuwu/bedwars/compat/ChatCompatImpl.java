package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

/**
 * Implementação padrão de chat usando Adventure nativa do Paper (1.16.5+).
 */
public final class ChatCompatImpl implements ChatCompat {

    @Override
    public void sendMessage(final @NotNull CommandSender sender, final @NotNull Component message) {
        sender.sendMessage(message);
    }

    @Override
    public void showTitle(final @NotNull Player player, final @NotNull Title title) {
        player.showTitle(title);
    }

    @Override
    public void clearTitle(final @NotNull Player player) {
        player.clearTitle();
    }

    @Override
    public void setDisplayName(final @NotNull ItemMeta meta, final @NotNull Component name) {
        meta.displayName(name);
    }

    @Override
    public void setLore(final @NotNull ItemMeta meta, final @NotNull java.util.List<Component> lore) {
        meta.lore(lore);
    }
}
