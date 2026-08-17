package dev.sebastianjnuwu.bedwars.game;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.BedWarsPlugin;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.ForgeLevel;
import dev.sebastianjnuwu.bedwars.api.model.UpgradeConfig;

/**
 * Responsável pelos upgrades da partida: forja (delegada a {@link GameForge}),
 * afinamento (sharpness) e proteção (protection). Aplica os encantamentos nos
 * jogadores do time.
 */
public final class GameUpgrades {

    private final Game game;
    private final GameForge forge;

    /**
     * Cria o gerenciador de upgrades para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameUpgrades(final Game game) {
        this.game = game;
        this.forge = new GameForge(game);
    }

    /**
     * Retorna o nível atual da forja.
     *
     * @param forge forja consultada (não nula)
     * @return nível atual (0 se nunca foi definido)
     */
    public int getForgeLevel(final ArenaGenerator forge) {
        return this.forge.getForgeLevel(forge);
    }

    /**
     * Retorna o próximo nível de forja disponível, ou {@code null} se já está no máximo.
     *
     * @param forge forja consultada (não nula)
     * @return próximo nível ou null
     */
    public @Nullable ForgeLevel getForgeUpgradeLevel(final ArenaGenerator forge) {
        return this.forge.getForgeUpgradeLevel(forge);
    }

    /**
     * Aumenta o nível da forja em 1, dispara o evento e reprograma os ticks.
     *
     * @param forge forja a ser melhorada (não nula)
     * @return {@code true} se o upgrade foi aplicado
     */
    public boolean upgradeForge(final ArenaGenerator forge) {
        return this.forge.upgradeForge(forge);
    }

    /**
     * Inicializa os ticks de forja e os níveis padrão de todas as forjas da arena.
     */
    public void initForgeTicks() {
        this.forge.initForgeTicks();
    }

    /**
     * Gera os drops de todos os itens programados nas forjas da partida.
     */
    public void handleForgeTicks() {
        this.forge.handleForgeTicks();
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

    /**
     * Retorna a configuração de níveis de um upgrade (ex.: {@code forge}) da loja da arena.
     *
     * @param upgrade identificador do upgrade (não nulo)
     * @return configuração de níveis ou {@code null}
     */
    @Nullable UpgradeConfig upgradeConfig(final String upgrade) {
        final String shop = this.game.arena.getShop() == null ? "default" : this.game.arena.getShop();
        return ((BedWarsPlugin) this.game.gameManager.getPlugin()).getShopManager().getUpgradeConfig(shop, upgrade);
    }

    private int getMaxSharpnessLevel() {
        final UpgradeConfig config = this.upgradeConfig("sharpness");
        return Math.max(1, config == null ? 0 : config.maxLevel());
    }

    private int getMaxProtectionLevel() {
        final UpgradeConfig config = this.upgradeConfig("protection");
        return Math.max(1, config == null ? 0 : config.maxLevel());
    }

    private static boolean isSword(final org.bukkit.Material material) {
        return material.name().endsWith("_SWORD");
    }
}