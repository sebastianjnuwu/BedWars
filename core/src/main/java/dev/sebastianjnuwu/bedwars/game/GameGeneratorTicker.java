package dev.sebastianjnuwu.bedwars.game;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import dev.sebastianjnuwu.bedwars.api.events.GeneratorSpawnEvent;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;

/**
 * Responsável pelos geradores de recursos da partida: inicialização dos ticks
 * de spawn, nível atual conforme o tempo decorrido e a geração de itens no
 * mundo (com limite de itens próximos e evento {@link GeneratorSpawnEvent}).
 */
public final class GameGeneratorTicker {

    private final Game game;

    /**
     * Cria o gerenciador de geradores para a partida informada.
     *
     * @param game partida que será alcançada por este gerenciador (não nula)
     */
    public GameGeneratorTicker(final Game game) {
        this.game = game;
    }

    /**
     * Inicializa os ticks de spawn de todos os geradores da arena.
     */
    void initGeneratorTicks() {
        this.game.generatorTicks.clear();
        for (final ArenaGenerator generator : this.game.arena.getGenerators()) {
            if (generator.getType().equalsIgnoreCase("forge")) {
                continue;
            }
            if (generator.getLocation() == null) {
                continue;
            }
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.game.arena.getGeneratorConfigs();
            if (genConfigs == null) {
                continue;
            }
            final GeneratorConfig config = genConfigs.get(type);
            if (config == null) {
                continue;
            }
            final Material material = config.material();
            final long interval = config.intervalForLevel(this.currentGeneratorLevel());
            if (material == null || interval <= 0L) {
                continue;
            }
            this.game.generatorTicks.put(generator, new long[]{0L, interval, 0L});
        }
    }

    /**
     * Processa um tick de geradores, gerando itens quando o intervalo é atingido.
     */
    void handleGeneratorTicks() {
        for (final Map.Entry<ArenaGenerator, long[]> entry : this.game.generatorTicks.entrySet()) {
            final ArenaGenerator generator = entry.getKey();
            if (generator.getLocation() == null) {
                continue;
            }
            final long[] data = entry.getValue();
            final long lastSpawn = data[0];
            final String type = generator.getType().toLowerCase();
            final var genConfigs = this.game.arena.getGeneratorConfigs();
            final GeneratorConfig config = genConfigs != null ? genConfigs.get(type) : null;
            final Material material = config != null ? config.material() : this.game.gameManager.getConfigManager().getGeneratorMaterial(type);
            if (material == null) {
                continue;
            }
            final long interval = config != null
                    ? config.intervalForLevel(this.currentGeneratorLevel())
                    : this.game.gameManager.getConfigManager().getGeneratorInterval(type);
            if (interval <= 0L || this.game.tick - lastSpawn < interval) {
                continue;
            }
            data[0] = this.game.tick;
            data[1] = interval;
            final var dropLocation = generator.getLocation().getBlock().getLocation().add(0.5, 1.2, 0.5);
            final long nearbyCount = dropLocation.getWorld().getNearbyEntities(dropLocation, 2, 2, 2).stream()
                    .filter(entity -> entity instanceof Item)
                    .filter(entity -> ((Item) entity).getItemStack().getType() == material)
                    .count();
            if (nearbyCount >= 32) {
                continue;
            }
            final ItemStack stack = new ItemStack(material);
            final GeneratorSpawnEvent spawnEvent = new GeneratorSpawnEvent(generator, stack);
            Bukkit.getPluginManager().callEvent(spawnEvent);
            if (spawnEvent.isCancelled()) {
                continue;
            }
            dropLocation.getWorld().dropItem(dropLocation, spawnEvent.getItem(), item -> {
                item.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
                item.setPickupDelay(0);
            });
        }
    }

    private int currentGeneratorLevel() {
        final Map<Integer, Integer> levelTimes = this.game.arena.getLevelTimes();
        if (levelTimes == null || levelTimes.isEmpty()) {
            return 1;
        }
        final int minutes = (int) (this.game.tick / (20L * 60L));
        int level = 1;
        for (final var entry : levelTimes.entrySet()) {
            if (entry.getKey() <= minutes && entry.getValue() > level) {
                level = entry.getValue();
            }
        }
        return level;
    }
}