package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Abstrai a IA de ataque dos golems convocados.
 * <p>
 * A Mob Goal API ({@code Bukkit.getMobGoals()}, {@code getPathfinder()},
 * {@code attack()}) só existe em Paper 1.20.6+. Em versões anteriores a
 * implementação usa fallback por tick ou NMS via reflection.
 * </p>
 */
public interface GolemCompat {

    /**
     * Registra a goal de ataque no golem.
     *
     * @param golem     golem convocado (não nulo)
     * @param ownerTeam time dono do golem (não nulo)
     * @param resolver  resolve o inimigo mais próximo (não nulo)
     */
    void registerAttackGoal(@NotNull IronGolem golem, @NotNull ArenaTeam ownerTeam,
                            @NotNull TargetResolver resolver);

    /**
     * Move uma entidade em direção a um alvo.
     *
     * @param golem  golem (não nulo)
     * @param target alvo (não nulo)
     * @param speed  velocidade de perseguição
     */
    void moveTo(@NotNull IronGolem golem, @NotNull LivingEntity target, double speed);

    /**
     * Aplica dano de ataque do golem no alvo.
     *
     * @param attacker golem atacante (não nulo)
     * @param target   alvo (não nulo)
     */
    void attack(@NotNull IronGolem attacker, @NotNull LivingEntity target);

    /**
     * Resolve o inimigo vivo mais próximo do golem dentro do alcance.
     */
    @FunctionalInterface
    interface TargetResolver {

        /**
         * @param golem     golem (não nulo)
         * @param ownerTeam time dono do golem (não nulo)
         * @return inimigo mais próximo ou {@code null}
         */
        @Nullable
        LivingEntity findNearestEnemy(@NotNull IronGolem golem, @NotNull ArenaTeam ownerTeam);
    }
}
