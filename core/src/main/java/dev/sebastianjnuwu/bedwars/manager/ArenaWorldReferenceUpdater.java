package dev.sebastianjnuwu.bedwars.manager;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;

/**
 * Re-sincroniza as referências de localização de uma arena (spawns, camas,
 * geradores e NPCs) para um mundo recém-construído, preservando yaw/pitch.
 */
final class ArenaWorldReferenceUpdater {

    private ArenaWorldReferenceUpdater() {
    }

    static void update(final Arena arena, final World newWorld) {
        if (arena.getArenaSpawn() != null) {
            final Location old = arena.getArenaSpawn();
            arena.setArenaSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        if (arena.getLobby() != null) {
            final Location old = arena.getLobby();
            arena.setLobby(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
        }
        for (final ArenaTeam team : arena.getTeams()) {
            if (team.getSpawn() != null) {
                final Location old = team.getSpawn();
                team.setSpawn(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
            if (team.getBed() != null) {
                final Location old = team.getBed();
                team.setBed(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        for (final ArenaGenerator gen : arena.getGenerators()) {
            if (gen.getLocation() != null) {
                final Location old = gen.getLocation();
                gen.setLocation(new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch()));
            }
        }
        if (arena.getShopNpcs() != null) {
            final List<ShopNpc> updated = new ArrayList<>();
            for (final ShopNpc npc : arena.getShopNpcs()) {
                final Location old = npc.location();
                final Location newLoc = new Location(newWorld, old.getX(), old.getY(), old.getZ(), old.getYaw(), old.getPitch());
                updated.add(new ShopNpc(newLoc, npc.skin(), npc.displayName()));
            }
            arena.setShopNpcs(updated);
        }
    }
}