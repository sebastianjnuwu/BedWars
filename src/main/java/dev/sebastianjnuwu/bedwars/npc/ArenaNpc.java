package dev.sebastianjnuwu.bedwars.npc;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class ArenaNpc {

    private final String id;
    private String skin;
    private String displayName;
    private String type; // ITEM_SHOP, UPGRADE_SHOP
    private Location location;

    public ArenaNpc(final String id) {
        this.id = id;
    }

    public String getId() { return this.id; }

    public @Nullable String getSkin() { return this.skin; }
    public void setSkin(final @Nullable String skin) { this.skin = skin; }

    public @Nullable String getDisplayName() { return this.displayName; }
    public void setDisplayName(final @Nullable String displayName) { this.displayName = displayName; }

    public @Nullable String getType() { return this.type; }
    public void setType(final @Nullable String type) { this.type = type; }

    public @Nullable Location getLocation() { return this.location; }
    public void setLocation(final @Nullable Location location) { this.location = location; }
}
