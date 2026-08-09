package dev.sebastianjnuwu.bedwars.compat;

import java.util.EnumSet;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Implementação padrão da IA de golem usando a Mob Goal API do Paper (1.20.6+).
 * <p>
 * Contorna a restrição vanilla de golems criados por jogador, que não
 * perseguem jogadores ({@code canTarget(Player)} é falso). Em vez de depender
 * do {@code setTarget}, a goal move o golem com o {@code Pathfinder} e aplica
 * o dano via {@code attack} diretamente, usando um cooldown próprio para não
 * repetir o golpe a cada tick.
 * </p>
 */
public final class GolemCompatImpl implements GolemCompat {

    private static final GoalKey<IronGolem> GOLEM_ATTACK_GOAL_KEY = GoalKey.of(IronGolem.class,
            new NamespacedKey("bedwars", "golem_attack"));

    @Override
    public void registerAttackGoal(final @NotNull IronGolem golem, final @NotNull ArenaTeam ownerTeam,
                                   final @NotNull TargetResolver resolver) {
        Bukkit.getMobGoals().addGoal(golem, 0, new GolemAttackGoal(golem, ownerTeam, resolver, this));
    }

    @Override
    public void moveTo(final @NotNull IronGolem golem, final @NotNull LivingEntity target, final double speed) {
        golem.getPathfinder().moveTo(target, speed);
    }

    @Override
    public void attack(final @NotNull IronGolem attacker, final @NotNull LivingEntity target) {
        attacker.attack(target);
    }

    /**
     * IA que faz um golem de ferro convocado perseguir e atacar o inimigo mais
     * próximo.
     */
    private static final class GolemAttackGoal implements Goal<IronGolem> {

        private static final int ATTACK_COOLDOWN = 20;
        private static final double CHASE_SPEED = 1.0D;
        private static final double ATTACK_RANGE_SQ = 4.0D;

        private final IronGolem golem;
        private final ArenaTeam ownerTeam;
        private final TargetResolver resolver;
        private final GolemCompat compat;
        private @Nullable LivingEntity target;
        private int attackCooldown;

        GolemAttackGoal(final IronGolem golem, final ArenaTeam ownerTeam,
                        final TargetResolver resolver, final GolemCompat compat) {
            this.golem = golem;
            this.ownerTeam = ownerTeam;
            this.resolver = resolver;
            this.compat = compat;
        }

        @Override
        public boolean shouldActivate() {
            this.target = this.resolver.findNearestEnemy(this.golem, this.ownerTeam);
            return this.target != null;
        }

        @Override
        public boolean shouldStayActive() {
            return this.target != null && this.target.isValid() && !this.target.isDead()
                    && this.golem.getWorld() == this.target.getWorld();
        }

        @Override
        public void start() {
            this.golem.setTarget(this.target);
        }

        @Override
        public void tick() {
            final LivingEntity current = this.resolver.findNearestEnemy(this.golem, this.ownerTeam);
            if (current != null) {
                this.target = current;
            }
            if (this.target == null) {
                return;
            }
            this.golem.setTarget(this.target);
            final double distanceSq = this.golem.getLocation().distanceSquared(this.target.getLocation());
            if (distanceSq <= ATTACK_RANGE_SQ) {
                this.golem.lookAt(this.target);
                if (this.attackCooldown <= 0) {
                    this.compat.attack(this.golem, this.target);
                    this.attackCooldown = ATTACK_COOLDOWN;
                }
            } else {
                this.compat.moveTo(this.golem, this.target, CHASE_SPEED);
            }
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
        }

        @Override
        public void stop() {
            this.golem.setTarget(null);
        }

        @Override
        public GoalKey<IronGolem> getKey() {
            return GOLEM_ATTACK_GOAL_KEY;
        }

        @Override
        public EnumSet<GoalType> getTypes() {
            return EnumSet.of(GoalType.MOVE, GoalType.LOOK, GoalType.TARGET);
        }
    }
}
