package dev.sebastianjnuwu.bedwars.model;

import java.util.UUID;

/**
 * Representa um jogador dentro de uma instância de partida de BedWars. Monitora
 * a atribuição de equipe, estado de vida, mortes causadas e mortes sofridas.
 */
public class GamePlayer implements dev.sebastianjnuwu.bedwars.api.model.GamePlayer {

    private final UUID uuid;
    private final dev.sebastianjnuwu.bedwars.api.model.ArenaTeam team;
    private boolean alive;
    private int deaths;
    private int kills;

    public GamePlayer(final UUID uuid, final dev.sebastianjnuwu.bedwars.api.model.ArenaTeam team) {
        this.uuid = uuid;
        this.team = team;
        this.alive = true;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public dev.sebastianjnuwu.bedwars.api.model.ArenaTeam getTeam() {
        return this.team;
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void setAlive(final boolean alive) {
        this.alive = alive;
    }

    public int getDeaths() {
        return this.deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public int getKills() {
        return this.kills;
    }

    public void addKill() {
        this.kills++;
    }
}
