package dev.sebastianjnuwu.bedwars.model;

import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Representa um time dentro de uma arena de BedWars.
 */
public class ArenaTeam implements dev.sebastianjnuwu.bedwars.api.model.ArenaTeam {

    private final String name;
    private final String color;
    private Location spawn;
    private String spawnBlockData;
    private Location bed;
    private String bedFacing;
    private ArenaGenerator forge;

    /**
     * Cria um time.
     *
     * @param name  nome do time (ex: "azul")
     * @param color cor do time (ex: "BLUE")
     */
    public ArenaTeam(final String name, final String color) {
        this.name = name;
        this.color = color;
    }

    /**
     * Retorna o nome do time.
     *
     * @return nome (ex: "azul")
     */
    public String getName() {
        return this.name;
    }

    /**
     * Retorna a cor do time.
     *
     * @return cor (ex: "BLUE")
     */
    public String getColor() {
        return this.color;
    }

    /**
     * Retorna o local de spawn do time.
     *
     * @return spawn ou null
     */
    public @Nullable Location getSpawn() {
        return this.spawn;
    }

    /**
     * Define o local de spawn do time.
     *
     * @param spawn local de spawn
     */
    public void setSpawn(final Location spawn) {
        this.spawn = spawn;
    }

    public @Nullable String getSpawnBlockData() {
        return this.spawnBlockData;
    }

    public void setSpawnBlockData(final String spawnBlockData) {
        this.spawnBlockData = spawnBlockData;
    }

    /**
     * Retorna o local da cama do time.
     *
     * @return local da cama ou null
     */
    public @Nullable Location getBed() {
        return this.bed;
    }

    /**
     * Define o local da cama do time.
     *
     * @param bed local da cama
     */
    public void setBed(final Location bed) {
        this.bed = bed;
    }

    /**
     * Retorna a direção que a cama está virada.
     *
     * @return direção (ex: "NORTH", "SOUTH") ou null
     */
    public @Nullable String getBedFacing() {
        return this.bedFacing;
    }

    /**
     * Define a direção da cama.
     *
     * @param bedFacing direção (ex: "NORTH")
     */
    public void setBedFacing(final String bedFacing) {
        this.bedFacing = bedFacing;
    }

    /**
     * Retorna a fornalha do time.
     */
    public @Nullable ArenaGenerator getForge() {
        return this.forge;
    }

    /**
     * Define a fornalha do time.
     *
     * @param forge gerador do tipo forge, ou null para remover
     */
    public void setForge(final @Nullable ArenaGenerator forge) {
        this.forge = forge;
    }
}
