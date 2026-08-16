package dev.sebastianjnuwu.bedwars.api.model;

import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Configuração de níveis de um upgrade de time (forja, afiação ou proteção),
 * lida da loja (shop.yml). Reutiliza {@link ForgeLevel} como nível, pois a
 * estrutura é a mesma: cada nível define o custo do próximo upgrade
 * ({@code upgrade.price}/{@code upgrade.material}) e, no caso da forja, os
 * intervalos de geração por material.
 *
 * @param maxLevel     nível máximo alcançável (o último nível não tem upgrade)
 * @param levelDefault nível inicial da forja ({@code 0} para afiação/proteção)
 * @param levels       configuração de cada nível
 */
public record UpgradeConfig(int maxLevel, int levelDefault, List<ForgeLevel> levels) {

    public UpgradeConfig(int maxLevel, List<ForgeLevel> levels) {
        this(maxLevel, 0, levels);
    }

    /**
     * Retorna a configuração do próximo nível a partir de um nível atual.
     *
     * @param currentLevel nível atual (0 quando o upgrade ainda não foi comprado)
     * @return nível seguinte ou {@code null} se não existir (upgrade no máximo)
     */
    public @Nullable ForgeLevel nextLevel(final int currentLevel) {
        final int next = currentLevel + 1;
        if (levels == null) {
            return null;
        }
        for (final ForgeLevel level : levels) {
            if (level.level() == next) {
                return level;
            }
        }
        return null;
    }
}