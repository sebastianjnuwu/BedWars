package dev.sebastianjnuwu.bedwars.model;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an item generator inside an arena. Each generator has
 * a type (bronze, iron, gold), a location, and the original block data
 * for block restoration.
 */
public class ArenaGenerator implements dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator {

    private final String type;
    private final Location location;
    private String team;
    private String originBlockData;
    private String originBlockDataAbove;

    public ArenaGenerator(final String type, final Location location) {
        this.type = type;
        this.location = location;
    }

    public String getType() {
        return this.type;
    }

    public Location getLocation() {
        return this.location;
    }

    public @Nullable String getTeam() {
        return this.team;
    }

    public void setTeam(final @Nullable String team) {
        this.team = team;
    }

    public @Nullable String getOriginBlockData() {
        return this.originBlockData;
    }

    public void setOriginBlockData(final String originBlockData) {
        this.originBlockData = originBlockData;
    }

    public @Nullable String getOriginBlockDataAbove() {
        return this.originBlockDataAbove;
    }

    public void setOriginBlockDataAbove(final String originBlockDataAbove) {
        this.originBlockDataAbove = originBlockDataAbove;
    }
}
