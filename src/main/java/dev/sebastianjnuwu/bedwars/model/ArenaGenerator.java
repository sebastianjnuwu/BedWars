package dev.sebastianjnuwu.bedwars.model;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Representa um gerador de itens dentro de uma arena. Cada gerador tem
 * um tipo (bronze, ferro, ouro), uma localização e os dados originais do bloco
 * para restauração de bloco.
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
