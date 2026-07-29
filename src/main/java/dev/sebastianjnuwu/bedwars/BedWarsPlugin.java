package dev.sebastianjnuwu.bedwars;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.BedWarsAPI;
import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.Game;
import dev.sebastianjnuwu.bedwars.api.model.GamePlayer;
import dev.sebastianjnuwu.bedwars.api.model.GameState;
import dev.sebastianjnuwu.bedwars.command.BWCommand;
import dev.sebastianjnuwu.bedwars.lang.LangManager;
import dev.sebastianjnuwu.bedwars.listener.ArenaListener;
import dev.sebastianjnuwu.bedwars.listener.GameListener;
import dev.sebastianjnuwu.bedwars.listener.UIListener;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import dev.sebastianjnuwu.bedwars.manager.ConfigManager;
import dev.sebastianjnuwu.bedwars.manager.GameManager;
import dev.sebastianjnuwu.bedwars.session.EditorManager;
import dev.sebastianjnuwu.bedwars.shop.ShopListener;
import dev.sebastianjnuwu.bedwars.shop.ShopManager;
import dev.sebastianjnuwu.bedwars.util.VersionChecker;
import dev.sebastianjnuwu.bedwars.world.WorldManager;

public class BedWarsPlugin extends JavaPlugin implements BedWarsAPI {

    private ArenaManager arenaManager;
    private EditorManager editorManager;
    private ConfigManager configManager;
    private GameManager gameManager;
    private LangManager lang;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.editorManager = new EditorManager(this);
        this.lang = new LangManager(this, this.configManager.getLang());

        final WorldManager worldManager = new WorldManager(this);
        final File mapsFolder = new File(this.getDataFolder(), "maps");
        mapsFolder.mkdirs();

        this.arenaManager = new ArenaManager(this, worldManager, mapsFolder);
        this.arenaManager.load();

        this.gameManager = new GameManager(this, this.arenaManager, this.configManager, this.lang, this.editorManager);

        this.shopManager = new ShopManager(this);
        this.shopManager.loadDefaults();

        this.getServer().getPluginManager().registerEvents(new ArenaListener(this.arenaManager, this.gameManager, this.editorManager), this);
        this.getServer().getPluginManager().registerEvents(new GameListener(this.gameManager), this);
        this.getServer().getPluginManager().registerEvents(new UIListener(this.arenaManager, this.gameManager), this);
        this.getServer().getPluginManager().registerEvents(new ShopListener(), this);

        // Register FancyNPCs listener if available
        try {
            Class.forName("de.oliver.fancynpcs.api.events.NpcInteractEvent");
            this.getServer().getPluginManager().registerEvents(
                    new dev.sebastianjnuwu.bedwars.shop.NpcListener(this.gameManager, this.shopManager, this.lang), this);
            this.getLogger().info(this.lang.raw("startup.fancynpcs_hook"));
        } catch (ClassNotFoundException e) {
            this.getLogger().info(this.lang.raw("startup.fancynpcs_not_found"));
        }

        final BWCommand bwCommand = new BWCommand(
                this.arenaManager,
                this.editorManager,
                this.configManager,
                this.gameManager,
                this.lang,
                mapsFolder
        );
        final var command = this.getCommand("bw");
        if (command != null) {
            command.setExecutor(bwCommand);
            command.setTabCompleter(bwCommand);
        }

        this.getLogger().info("\n\n" +
                "██████╗ ███████╗██████╗ ██╗    ██╗ █████╗ ██████╗ ███████╗\n" +
                "██╔══██╗██╔════╝██╔══██╗██║    ██║██╔══██╗██╔══██╗██╔════╝\n" +
                "██████╔╝█████╗  ██║  ██║██║ █╗ ██║███████║██████╔╝███████╗\n" +
                "██╔══██╗██╔══╝  ██║  ██║██║███╗██║██╔══██║██╔══██╗╚════██║\n" +
                "██████╔╝███████╗██████╔╝╚███╔███╔╝██║  ██║██║  ██║███████║\n" +
                "╚═════╝ ╚══════╝╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝\n" +
                "                                                           \n" +
                "  - GitHub: " + this.lang.raw("startup.github") + "\n" +
                "  - Author: " + this.lang.raw("startup.author") + "\n" +
                "  - Version: " + this.lang.raw("startup.version", getDescription().getVersion()) +
                "\n");

        if (this.configManager.isVersionCheckEnabled()) {
            new VersionChecker(
                    getDescription().getVersion(),
                    this.getLogger(),
                    this.lang
            ).checkAsync();
        } else {
            this.getLogger().info(this.lang.raw("version_check.disabled"));
        }

    }

    @Override
    public void onDisable() {

        if (this.editorManager != null) {
            this.editorManager.shutdown(this.configManager, this.arenaManager, this.lang);
        }

        if (this.gameManager != null) {
            this.gameManager.forceEndAll();
        }

        if (this.arenaManager != null) {
            this.arenaManager.flush();
        }

        this.getLogger().info("BedWars desativado!");
    }

    @Override
    public @Nullable
    dev.sebastianjnuwu.bedwars.api.model.Game getGame(final @NotNull String arenaName) {
        return this.gameManager.getGame(arenaName);
    }

    @Override
    public @Nullable
    dev.sebastianjnuwu.bedwars.api.model.Game getPlayerGame(final @NotNull Player player) {
        return this.gameManager.getPlayerGame(player);
    }

    @Override
    public boolean isInGame(final @NotNull Player player) {
        return this.gameManager.isInGame(player);
    }

    @Override
    public @Nullable
    GamePlayer getGamePlayer(final @NotNull Player player) {
        final Game game = this.gameManager.getPlayerGame(player);
        return game != null ? game.getGamePlayer(player) : null;
    }

    @Override
    public @NotNull
    dev.sebastianjnuwu.bedwars.api.ArenaManager getArenaManager() {
        return this.arenaManager;
    }

    @Override
    public @NotNull
    dev.sebastianjnuwu.bedwars.api.GameManager getGameManager() {
        return this.gameManager;
    }

    @Override
    public @NotNull
    Collection<Arena> getArenas() {
        return this.arenaManager.getAll();
    }

    @Override
    public @Nullable
    Arena getArena(final @NotNull String name) {
        return this.arenaManager.get(name);
    }

    @Override
    public boolean forceStart(final @NotNull String arenaName) {
        final Game game = this.gameManager.getGame(arenaName);
        if (game == null) {
            return false;
        }
        if (game.getState() != GameState.WAITING && game.getState() != GameState.STARTING) {
            return false;
        }
        game.start();
        return true;
    }

    @Override
    public boolean forceEnd(final @NotNull String arenaName) {
        final Game game = this.gameManager.getGame(arenaName);
        if (game == null) {
            return false;
        }
        game.forceEnd();
        return true;
    }

    @Override
    public boolean addPlayer(final @NotNull Player player, final @NotNull String arenaName) {
        if (this.gameManager.isInGame(player)) {
            return false;
        }
        this.gameManager.joinGame(player, arenaName);
        return this.gameManager.isInGame(player);
    }

    @Override
    public boolean addPlayer(final @NotNull Player player, final @NotNull String arenaName, final @NotNull String teamName) {
        if (this.gameManager.isInGame(player)) {
            return false;
        }
        this.gameManager.joinGame(player, arenaName, teamName);
        return this.gameManager.isInGame(player);
    }

    @Override
    public boolean removePlayer(final @NotNull Player player) {
        if (!this.gameManager.isInGame(player)) {
            return false;
        }
        this.gameManager.leaveGame(player);
        return true;
    }

    @Override
    public @NotNull Collection<Player> getPlayersInArena(@NotNull String arenaName) {
        Game game = this.gameManager.getGame(arenaName);
        return game != null ? game.getPlayers() : Collections.emptyList();
    }

    @Override
    public void broadcast(@NotNull String arenaName, @NotNull String message) {
        Game game = this.gameManager.getGame(arenaName);
        if (game != null) {
            game.broadcast(message);
        }
    }

    public EditorManager getEditorManager() {
        return this.editorManager;
    }

    public LangManager getLang() {
        return this.lang;
    }
}
