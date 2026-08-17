package dev.sebastianjnuwu.bedwars.game.upgrade;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.events.GeneratorSpawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorUpgradeEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;
import dev.sebastianjnuwu.bedwars.game.Game;

/**
 * Responsável pela forja (upgrade de time): níveis, programação de drops por
 * material e geração de itens no mundo. Lê a configuração de níveis da loja
 * da arena via {@link GameUpgrades#upgradeConfig(String)}.
 */
public final class GameForge {

    private final Game game;

    /**
     * Cria o gerenciador da forja para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameForge(final Game game) {
        this.game = game;
    }

    /**
     * Retorna o nível atual da forja.
     *
     * @param forge forja consultada (não nula)
     * @return nível atual (0 se nunca foi definido)
     */
    public int getForgeLevel(final ArenaGenerator forge) {
        return this.game.forgeLevels.getOrDefault(forge, 0);
    }

    /**
     * Retorna o próximo nível de forja disponível, ou {@code null} se já está no máximo.
     *
     * @param forge forja consultada (não nula)
     * @return próximo nível ou null
     */
    public @Nullable ForgeLevel getForgeUpgradeLevel(final ArenaGenerator forge) {
        final UpgradeConfig config = this.game.upgrades().upgradeConfig("forge");
        if (config == null) {
            return null;
        }
        return config.nextLevel(this.getForgeLevel(forge));
    }

    /**
     * Aumenta o nível da forja em 1, dispara o evento e reprograma os ticks.
     *
     * @param forge forja a ser melhorada (não nula)
     * @return {@code true} se o upgrade foi aplicado
     */
    public boolean upgradeForge(final ArenaGenerator forge) {
        final Integer level = this.game.forgeLevels.get(forge);
        if (level == null || level >= this.getForgeMaxLevel()) {
            return false;
        }
        this.game.forgeLevels.put(forge, level + 1);
        Bukkit.getPluginManager().callEvent(new GeneratorUpgradeEvent(this.game, forge, level, level + 1));
        this.rescheduleForge(forge);
        return true;
    }

    /**
     * Inicializa os ticks de forja e os níveis padrão de todas as forjas da arena.
     */
    public void initForgeTicks() {
        this.game.forgeTicks.clear();
        this.game.forgeLevels.clear();
        for (final ArenaGenerator forge : this.game.arena.getGenerators()) {
            if (!forge.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (forge.getLocation() == null) {
                this.game.gameManager.getPlugin().getLogger().warning(this.game.lang.raw("log.game.forge_skipped", forge.getUniqueId()));
                continue;
            }
            this.game.forgeLevels.put(forge, Math.max(1, this.forgeDefaultLevel()));
            this.putForgeTicks(forge, this.game.forgeLevels.get(forge));
        }
    }

    /**
     * Gera os drops de todos os itens programados nas forjas da partida.
     */
    public void handleForgeTicks() {
        for (final Map.Entry<String, long[]> entry : this.game.forgeTicks.entrySet()) {
            final long[] data = entry.getValue();
            final long lastSpawn = data[0];
            final long interval = data[1];
            if (this.game.tick - lastSpawn < interval) {
                continue;
            }
            data[0] = this.game.tick;
            final String key = entry.getKey();
            final int colon = key.indexOf(':');
            if (colon == -1) {
                continue;
            }
            final String locKey = key.substring(0, colon);
            final String matName = key.substring(colon + 1);
            final Material material = Material.matchMaterial(matName);
            if (material == null) {
                continue;
            }
            final ArenaGenerator forge = this.findForgeByKey(locKey);
            if (forge == null || forge.getLocation() == null) {
                continue;
            }
            final Location dropLocation = forge.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final World world = dropLocation.getWorld();
            if (world == null) {
                continue;
            }
            final long nearbyCount = world.getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(e -> e instanceof Item)
                    .filter(e -> ((Item) e).getItemStack().getType() == material)
                    .count();
            if (nearbyCount >= 32) {
                continue;
            }
            final ItemStack stack = new ItemStack(material);
            final GeneratorSpawnEvent spawnEvent = new GeneratorSpawnEvent(forge, stack);
            Bukkit.getPluginManager().callEvent(spawnEvent);
            if (spawnEvent.isCancelled()) {
                continue;
            }
            world.dropItem(dropLocation, spawnEvent.getItem(), item -> {
                item.setVelocity(new Vector(0, 0, 0));
                item.setPickupDelay(0);
            });
        }
    }

    private int forgeDefaultLevel() {
        final UpgradeConfig config = this.game.upgrades().upgradeConfig("forge");
        return Math.max(1, config == null ? 1 : config.levelDefault());
    }

    private int getForgeMaxLevel() {
        final UpgradeConfig config = this.game.upgrades().upgradeConfig("forge");
        return Math.max(1, config == null ? 1 : config.maxLevel());
    }

    private void putForgeTicks(final ArenaGenerator forge, final int level) {
        Map<Material, Long> intervals = null;
        final UpgradeConfig config = this.game.upgrades().upgradeConfig("forge");
        if (config != null) {
            for (final ForgeLevel fl : config.levels()) {
                if (fl.level() == level) {
                    intervals = fl.intervals();
                    break;
                }
            }
        }
        if (intervals == null || intervals.isEmpty()) {
            this.game.gameManager.getPlugin().getLogger().warning(this.game.lang.raw("log.game.put_forge_ticks_warning", forge.getUniqueId(), level, config == null ? "null" : config.levels().size()));
            return;
        }
        for (final var entry : intervals.entrySet()) {
            final Material material = entry.getKey();
            final long interval = entry.getValue();
            final String key = forgeKey(forge) + ":" + material.name();
            this.game.forgeTicks.put(key, new long[]{0L, interval, 0L});
        }
    }

    private void rescheduleForge(final ArenaGenerator forge) {
        final int level = this.game.forgeLevels.getOrDefault(forge, 1);
        final String prefix = forgeKey(forge) + ":";
        this.game.forgeTicks.keySet().removeIf(k -> k.startsWith(prefix));
        this.putForgeTicks(forge, level);
    }

    private static String forgeKey(final ArenaGenerator forge) {
        final Location loc = forge.getLocation();
        return loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private @Nullable ArenaGenerator findForgeByKey(final String locKey) {
        for (final ArenaGenerator gen : this.game.arena.getGenerators()) {
            if (!gen.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (gen.getLocation() == null) {
                continue;
            }
            if (forgeKey(gen).equals(locKey)) {
                return gen;
            }
        }
        return null;
    }
}
