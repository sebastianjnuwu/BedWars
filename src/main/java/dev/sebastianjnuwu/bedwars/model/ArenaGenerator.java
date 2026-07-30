package dev.sebastianjnuwu.bedwars.model;

import java.util.UUID;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class ArenaGenerator implements dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator {

    private final UUID uniqueId;
    private final String type;
    private Location location;
    private String team;
    private String originBlockData;
    private String originBlockDataAbove;

    public ArenaGenerator(final String type, final Location location) {
        this.uniqueId = UUID.randomUUID();
        this.type = type;
        this.location = location;
    }

    public ArenaGenerator(final UUID uniqueId, final String type, final Location location) {
        this.uniqueId = uniqueId;
        this.type = type;
        this.location = location;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getType() {
        return this.type;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
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
