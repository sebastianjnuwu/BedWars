package dev.sebastianjnuwu.bedwars.compat;

import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;

/**
 * Implementação legada da IA de golem para versões anteriores à introdução da
 * Mob Goal API do Paper (1.20.6), onde não existem {@code Bukkit.getMobGoals()},
 * {@code getPathfinder()} nem {@code LivingEntity#attack(Entity)}.
 * <p>
 * Como o golem é convocado com {@code setPlayerCreated(false)}, a IA vanilla já
 * consegue mirar jogadores; este fallback apenas reaplica o alvo periodicamente
 * a partir do resolver (mesmo filtro de time do {@code GameListener}) e deixa o
 * vanilla cuidar da perseguição e do dano.
 * </p>
 */
public final class GolemCompatLegacy implements GolemCompat {

    private static final long TARGET_REFRESH_TICKS = 4L;

    @Override
    public void registerAttackGoal(final @NotNull IronGolem golem, final @NotNull ArenaTeam ownerTeam,
                                   final @NotNull TargetResolver resolver) {
        final JavaPlugin plugin = JavaPlugin.getProvidingPlugin(GolemCompatLegacy.class);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!golem.isValid() || golem.isDead()) {
                    this.cancel();
                    return;
                }
                final LivingEntity enemy = resolver.findNearestEnemy(golem, ownerTeam);
                if (enemy != null && golem.getWorld() == enemy.getWorld()) {
                    golem.setTarget(enemy);
                } else {
                    golem.setTarget(null);
                }
            }
        }.runTaskTimer(plugin, 0L, TARGET_REFRESH_TICKS);
    }

    @Override
    public void moveTo(final @NotNull IronGolem golem, final @NotNull LivingEntity target, final double speed) {
    }

    @Override
    public void attack(final @NotNull IronGolem attacker, final @NotNull LivingEntity target) {
        target.damage(attacker.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE) == null
                ? 6.0D
                : attacker.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE).getValue());
    }
}
