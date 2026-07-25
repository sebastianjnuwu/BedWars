package dev.sebastianjnuwu.bedwars.session;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.manager.ArenaManager;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Spawns coloured dust particles above every defined point of an arena
 * while a player is in editor mode.
 *
 * Particle legend:
 *   Arena spawn  → green
 *   Team spawns  → team colour (matches the wool block used as marker)
 *   Team beds    → red
 *   Generators   → yellow (iron/gold) | cyan (diamond) | purple (emerald) | orange (forge)
 *
 * The task is scheduled as a repeating BukkitTask and cancelled automatically
 * when the editor session ends.
 */
public class EditorParticleTask implements Runnable {

    private static final double PARTICLE_Y_OFFSET = 1.8;
    private static final float  PARTICLE_SIZE     = 1.2f;
    private static final double PARTICLE_SPREAD   = 0.15;
    private static final int    PARTICLE_COUNT    = 4;

    /** Maps team colour names (lower-case) to dust colours. */
    private static final Map<String, Color> TEAM_COLORS = Map.of(
            "azul",      Color.BLUE,
            "vermelho",  Color.RED,
            "verde",     Color.GREEN,
            "amarelo",   Color.YELLOW,
            "roxo",      Color.PURPLE,
            "rosa",      Color.fromRGB(255, 105, 180),
            "laranja",   Color.ORANGE,
            "ciano",     Color.AQUA
    );

    private final Player       editor;
    private final String       arenaName;
    private final ArenaManager arenaManager;

    public EditorParticleTask(final Player editor,
                              final String arenaName,
                              final ArenaManager arenaManager) {
        this.editor       = editor;
        this.arenaName    = arenaName;
        this.arenaManager = arenaManager;
    }

    @Override
    public void run() {
        if (!this.editor.isOnline()) return;

        final Arena arena = this.arenaManager.get(this.arenaName);
        if (arena == null) return;

        // ── Arena spawn (green) ──────────────────────────────────────────
        spawnDust(arena.getArenaSpawn(), Color.GREEN);

        // ── Teams ────────────────────────────────────────────────────────
        for (final ArenaTeam team : arena.getTeams()) {
            final Color teamColor = TEAM_COLORS.getOrDefault(
                    team.getName().toLowerCase(), Color.WHITE);

            // Team spawn → team colour
            spawnDust(team.getSpawn(), teamColor);

            // Team bed → red
            spawnDust(team.getBed(), Color.RED);
        }

        // ── Generators ───────────────────────────────────────────────────
        for (final ArenaGenerator gen : arena.getGenerators()) {
            final Color genColor = generatorColor(gen.getType());
            spawnDust(gen.getLocation(), genColor);
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private void spawnDust(final Location loc, final Color color) {
        if (loc == null) return;
        final World world = loc.getWorld();
        if (world == null) return;

        final Location center = loc.clone().add(0.5, PARTICLE_Y_OFFSET, 0.5);
        final Particle.DustOptions dust = new Particle.DustOptions(color, PARTICLE_SIZE);

        world.spawnParticle(
                Particle.DUST,
                center,
                PARTICLE_COUNT,
                PARTICLE_SPREAD, PARTICLE_SPREAD, PARTICLE_SPREAD,
                0,
                dust,
                true   // force-send to all players in range
        );
    }

    private static Color generatorColor(final String type) {
        return switch (type.toLowerCase()) {
            case "iron"              -> Color.fromRGB(200, 200, 200); // light grey
            case "gold"              -> Color.YELLOW;
            case "diamond"           -> Color.AQUA;
            case "emerald"           -> Color.fromRGB(0, 200, 80);   // bright green
            case "forge"             -> Color.ORANGE;
            default                  -> Color.WHITE;
        };
    }
}
