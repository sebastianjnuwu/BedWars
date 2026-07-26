package dev.sebastianjnuwu.bedwars.npc;

import dev.sebastianjnuwu.bedwars.manager.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class NpcInteractListener implements Listener {

    private final GameManager gameManager;
    private final Set<Integer> npcEntityIds = new CopyOnWriteArraySet<>();

    public NpcInteractListener(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void trackNpc(final int entityId) {
        this.npcEntityIds.add(entityId);
    }

    public void untrackNpc(final int entityId) {
        this.npcEntityIds.remove(entityId);
    }

    @EventHandler
    public void onNpcInteract(final PlayerInteractEntityEvent event) {
        if (!FancyNpcsHook.isAvailable()) return;
        if (!this.npcEntityIds.contains(event.getRightClicked().getEntityId())) return;
        event.setCancelled(true);
        final Player player = event.getPlayer();
        try {
            final Object fNpc = FancyNpcsHook.getNpc(event.getRightClicked().getEntityId());
            if (fNpc == null) return;
            final String name = FancyNpcsHook.getNpcName(fNpc);
            if (name == null) return;

            for (final var arena : this.gameManager.getArenaManager().getAll()) {
                if (name.startsWith("bw_" + arena.getName())) {
                    if (name.endsWith("_shop")) {
                        player.sendMessage(Component.text("Abrindo loja de itens...", NamedTextColor.GREEN));
                    } else if (name.endsWith("_upgrade")) {
                        player.sendMessage(Component.text("Abrindo loja de upgrades...", NamedTextColor.GREEN));
                    }
                    return;
                }
            }
        } catch (final Exception ignored) {
        }
    }
}
