package dev.sebastianjnuwu.bedwars.api.model;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

/**
 * Representa um NPC de loja configurado em uma arena.
 *
 * @param location    localização do NPC
 * @param skin        nome da skin ou null para padrão
 * @param displayName nome de exibição ou null para padrão
 */
public record ShopNpc(Location location, @Nullable String skin, @Nullable String displayName) {
}
