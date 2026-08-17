package dev.sebastianjnuwu.bedwars.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import dev.sebastianjnuwu.bedwars.manager.GameManager;

/**
 * Fachada dos listeners da partida de BedWars.
 * <p>
 * Registra os listeners temáticos responsáveis por combate, itens, golems e
 * blocos/jogadores durante uma partida ativa.
 * </p>
 */
public class GameListener implements Listener {

    private final GameManager gameManager;

    public GameListener(final GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void registerAll() {
        final JavaPlugin plugin = this.gameManager.getPlugin();
        Bukkit.getPluginManager().registerEvents(new GameCombatListener(this.gameManager), plugin);
        Bukkit.getPluginManager().registerEvents(new GameItemListener(this.gameManager), plugin);
        Bukkit.getPluginManager().registerEvents(new GameGolemListener(this.gameManager), plugin);
        Bukkit.getPluginManager().registerEvents(new GamePlayerListener(this.gameManager), plugin);
    }
}