package dev.sebastianjnuwu.bedwars.manager.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * Codifica e decodifica localizações da arena no formato do YAML
 * ({@code mundo,x,y,z,yaw,pitch}).
 */
final class ArenaLocationCodec {

    private ArenaLocationCodec() {
    }

    static String serialize(final Location loc) {
        return loc.getWorld().getName()
                + "," + loc.getBlockX()
                + "," + loc.getBlockY()
                + "," + loc.getBlockZ()
                + "," + loc.getYaw()
                + "," + loc.getPitch();
    }

    @Nullable static Location parseFor(final String str, final @Nullable World targetWorld) {
        if (targetWorld != null) {
            return rebase(str, targetWorld);
        }
        return parse(str);
    }

    /**
     * Converte uma string de localização em um {@link Location} do mundo alvo,
     * mantendo as coordenadas originais (usado ao construir instâncias).
     */
    private static @Nullable Location rebase(final String str, final World world) {
        if (str == null || str.isBlank()) {
            return null;
        }
        final String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }
        return new Location(world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                parts.length > 4 ? Float.parseFloat(parts[4]) : 0F,
                parts.length > 5 ? Float.parseFloat(parts[5]) : 0F);
    }

    private static @Nullable Location parse(final String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        final String[] parts = str.split(",");
        if (parts.length < 4) {
            return null;
        }
        final World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        return new Location(
                world,
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                parts.length > 4 ? Float.parseFloat(parts[4]) : 0F,
                parts.length > 5 ? Float.parseFloat(parts[5]) : 0F
        );
    }
}
