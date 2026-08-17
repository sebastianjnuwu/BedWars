package dev.sebastianjnuwu.bedwars.game;

import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorSpawnEvent;
import dev.sebastianjnuwu.bedwars.api.events.GeneratorUpgradeEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;

/**
 * Responsável pelos upgrades da partida: forja (níveis e ticks de geração),
 * afinamento (sharpness) e proteção (protection). Aplica os encantamentos nos
 * jogadores do time e gerencia a programação de drops da forja.
 */
public final class GameUpgrades {

    private final Game game;

    /**
     * Cria o gerenciador de upgrades para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameUpgrades(final Game game) {
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
        final UpgradeConfig config = this.upgradeConfig("forge");
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
     * Retorna o nível de afinamento (sharpness) do time.
     *
     * @param team time consultado (não nulo)
     * @return nível atual (0 se nunca foi definido)
     */
    public int getSharpnessLevel(final ArenaTeam team) {
        return this.game.sharpnessLevels.getOrDefault(team, 0);
    }

    /**
     * Aumenta o nível de afinamento do time em 1 e reaplica os encantamentos.
     *
     * @param team time a ser melhorado (não nulo)
     * @return {@code true} se o upgrade foi aplicado
     */
    public boolean upgradeSharpness(final ArenaTeam team) {
        final int level = this.getSharpnessLevel(team);
        if (level >= this.getMaxSharpnessLevel()) {
            return false;
        }
        this.game.sharpnessLevels.put(team, level + 1);
        this.applySharpnessToTeam(team);
        return true;
    }

    /**
     * Aplica o encantamento de afinamento nas espadas do inventário dos jogadores do time.
     *
     * @param team time cujas espadas serão encantadas (não nulo)
     */
    public void applySharpnessToTeam(final ArenaTeam team) {
        final int level = this.getSharpnessLevel(team);
        if (level <= 0) {
            return;
        }
        for (final var uuid : this.game.teams.getOrDefault(team, List.of())) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            for (final ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && isSword(stack.getType())) {
                    stack.addUnsafeEnchantment(Enchantment.SHARPNESS, level);
                }
            }
        }
    }

    /**
     * Retorna o nível de proteção do time.
     *
     * @param team time consultado (não nulo)
     * @return nível atual (0 se nunca foi definido)
     */
    public int getProtectionLevel(final ArenaTeam team) {
        return this.game.protectionLevels.getOrDefault(team, 0);
    }

    /**
     * Aumenta o nível de proteção do time em 1 e reaplica os encantamentos.
     *
     * @param team time a ser melhorado (não nulo)
     * @return {@code true} se o upgrade foi aplicado
     */
    public boolean upgradeProtection(final ArenaTeam team) {
        final int level = this.getProtectionLevel(team);
        if (level >= this.getMaxProtectionLevel()) {
            return false;
        }
        this.game.protectionLevels.put(team, level + 1);
        this.applyProtectionToTeam(team);
        return true;
    }

    /**
     * Aplica o encantamento de proteção na armadura dos jogadores do time.
     *
     * @param team time cuja armadura será encantada (não nulo)
     */
    public void applyProtectionToTeam(final ArenaTeam team) {
        final int level = this.getProtectionLevel(team);
        if (level <= 0) {
            return;
        }
        for (final var uuid : this.game.teams.getOrDefault(team, List.of())) {
            final Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            for (final ItemStack stack : player.getInventory().getArmorContents()) {
                if (stack != null) {
                    stack.addUnsafeEnchantment(Enchantment.PROTECTION, level);
                }
            }
        }
    }

    /**
     * Retorna o nível máximo de upgrade (usa o máximo de sharpness).
     *
     * @return nível máximo de upgrade
     */
    public int getMaxUpgradeLevel() {
        return this.getMaxSharpnessLevel();
    }

    /**
     * Retorna o próximo nível de sharpness disponível para o time, ou {@code null}.
     *
     * @param team time consultado (não nulo)
     * @return próximo nível ou null
     */
    public @Nullable ForgeLevel getSharpnessUpgradeLevel(final ArenaTeam team) {
        final UpgradeConfig config = this.upgradeConfig("sharpness");
        if (config == null) {
            return null;
        }
        return config.nextLevel(this.getSharpnessLevel(team));
    }

    /**
     * Retorna o próximo nível de proteção disponível para o time, ou {@code null}.
     *
     * @param team time consultado (não nulo)
     * @return próximo nível ou null
     */
    public @Nullable ForgeLevel getProtectionUpgradeLevel(final ArenaTeam team) {
        final UpgradeConfig config = this.upgradeConfig("protection");
        if (config == null) {
            return null;
        }
        return config.nextLevel(this.getProtectionLevel(team));
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

    private int forgeDefaultLevel() {
        final UpgradeConfig config = this.upgradeConfig("forge");
        return Math.max(1, config == null ? 1 : config.levelDefault());
    }

    private void putForgeTicks(final ArenaGenerator forge, final int level) {
        Map<Material, Long> intervals = null;
        final UpgradeConfig config = this.upgradeConfig("forge");
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

    private int getForgeMaxLevel() {
        final UpgradeConfig config = this.upgradeConfig("forge");
        return Math.max(1, config == null ? 1 : config.maxLevel());
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
            final ArenaGenerator forge = findForgeByKey(locKey);
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

    private @Nullable UpgradeConfig upgradeConfig(final String upgrade) {
        final String shop = this.game.arena.getShop() == null ? "default" : this.game.arena.getShop();
        return ((BedWarsPlugin) this.game.gameManager.getPlugin()).getShopManager().getUpgradeConfig(shop, upgrade);
    }

    private static boolean isSword(final Material material) {
        return material.name().endsWith("_SWORD");
    }

    private int getMaxSharpnessLevel() {
        final UpgradeConfig config = this.upgradeConfig("sharpness");
        return Math.max(1, config == null ? 0 : config.maxLevel());
    }

    private int getMaxProtectionLevel() {
        final UpgradeConfig config = this.upgradeConfig("protection");
        return Math.max(1, config == null ? 0 : config.maxLevel());
    }

    /**
     * Aplica os upgrades de sharpness/protection já comprados nos itens do jogador.
     *
     * @param player jogador cujo inventário será encantado (não nulo)
     * @param team   time que define os níveis (não nulo)
     */
    public void applyTeamUpgrades(final Player player, final ArenaTeam team) {
        final int sharpness = this.getSharpnessLevel(team);
        if (sharpness > 0) {
            for (final ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && isSword(stack.getType())) {
                    stack.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpness);
                }
            }
        }
        final int protection = this.getProtectionLevel(team);
        if (protection > 0) {
            for (final ItemStack stack : player.getInventory().getArmorContents()) {
                if (stack != null) {
                    stack.addUnsafeEnchantment(Enchantment.PROTECTION, protection);
                }
            }
        }
    }
}