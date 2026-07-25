package dev.sebastianjnuwu.bedwars.command.admin.validator;

import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class ArenaEditorValidator {

    private final ArenaManager arenaManager;
    private final EditorManager editorManager;
    private final LangManager lang;

    public ArenaEditorValidator(
            final ArenaManager arenaManager,
            final EditorManager editorManager,
            final LangManager lang
    ) {
        this.arenaManager = arenaManager;
        this.editorManager = editorManager;
        this.lang = lang;
    }

    public @Nullable Arena validate(final Player player, final String arenaName) {
        final Arena arena = this.arenaManager.get(arenaName);
        if (arena == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "edit.not_found", arenaName));
            return null;
        }
        if (this.editorManager.isBeingEdited(arenaName) && !this.editorManager.isEditing(player, arenaName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "save.not_editing",
                    this.editorManager.getEditorName(arenaName)));
            return null;
        }
        final String worldName = arena.getWorldName();
        if (worldName == null) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "edit.not_loaded", arenaName));
            return null;
        }
        if (!player.getWorld().getName().equals(worldName)) {
            player.sendMessage(this.lang.text(NamedTextColor.RED, "admin.arena.not_in_world", arenaName));
            return null;
        }
        return arena;
    }
}
