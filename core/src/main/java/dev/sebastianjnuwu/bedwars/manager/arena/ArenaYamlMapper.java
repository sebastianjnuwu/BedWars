package dev.sebastianjnuwu.bedwars.manager.arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import dev.sebastianjnuwu.bedwars.api.model.Arena;
import dev.sebastianjnuwu.bedwars.api.model.ArenaGenerator;
import dev.sebastianjnuwu.bedwars.api.model.ArenaTeam;
import dev.sebastianjnuwu.bedwars.api.model.GeneratorConfig;
import dev.sebastianjnuwu.bedwars.api.model.ShopNpc;

/**
 * Mapeia as seções do YAML de uma arena para/do modelo em memória.
 * <p>
 * Concentra a conversão de localizações, times, geradores, NPCs da loja,
 * configurações de geradores e level-times, preservando seções do disco quando
 * o mundo da arena ainda não está carregado.
 * </p>
 */
final class ArenaYamlMapper {

    void readSpawnItems(final YamlConfiguration config, final Arena arena) {
        if (!config.contains("spawn_item")) {
            return;
        }
        final List<Material> spawnItems = new ArrayList<>();
        for (final String itemName : config.getStringList("spawn_item")) {
            final Material mat = Material.matchMaterial(itemName);
            if (mat != null) {
                spawnItems.add(mat);
            }
        }
        arena.setSpawnItems(spawnItems);
    }

    void readTeams(final YamlConfiguration config, final Arena arena, final @Nullable World targetWorld) {
        if (!config.contains("teams")) {
            return;
        }
        for (final String key : config.getConfigurationSection("teams").getKeys(false)) {
            if (key.equalsIgnoreCase("min-players") || key.equalsIgnoreCase("max-players") || key.equalsIgnoreCase("min-teams")) {
                continue;
            }
            final String path = "teams." + key;
            final var team = new dev.sebastianjnuwu.bedwars.model.ArenaTeam(key, config.getString(path + ".color"));
            if (config.contains(path + ".spawn")) {
                team.setSpawn(ArenaLocationCodec.parseFor(config.getString(path + ".spawn"), targetWorld));
            }
            if (config.contains(path + ".spawn_block")) {
                team.setSpawnBlockData(config.getString(path + ".spawn_block"));
            }
            if (config.contains(path + ".bed")) {
                team.setBed(ArenaLocationCodec.parseFor(config.getString(path + ".bed"), targetWorld));
            }
            if (config.contains(path + ".bed_facing")) {
                team.setBedFacing(config.getString(path + ".bed_facing"));
            }
            arena.addTeam(team);
        }
    }

    void readGenerators(final YamlConfiguration config, final Arena arena, final @Nullable World targetWorld) {
        if (!config.contains("generators")) {
            return;
        }
        for (final String key : config.getConfigurationSection("generators").getKeys(false)) {
            final String path = "generators." + key;
            final String type = config.getString(path + ".type");
            if (type == null) {
                continue;
            }

            UUID genUuid;
            try {
                genUuid = UUID.fromString(key);
            } catch (final IllegalArgumentException e) {
                genUuid = UUID.randomUUID();
            }

            final Location loc = ArenaLocationCodec.parseFor(config.getString(path + ".location"), targetWorld);
            final var gen = new dev.sebastianjnuwu.bedwars.model.ArenaGenerator(genUuid, type, loc);
            if (config.contains(path + ".team")) {
                gen.setTeam(config.getString(path + ".team"));
            }
            if (config.contains(path + ".origin_block")) {
                gen.setOriginBlockData(config.getString(path + ".origin_block"));
            }
            if (config.contains(path + ".origin_block_above")) {
                gen.setOriginBlockDataAbove(config.getString(path + ".origin_block_above"));
            }
            arena.addGenerator(gen);
        }
    }

    void readShopNpcs(final YamlConfiguration config, final Arena arena, final @Nullable World targetWorld) {
        if (!config.contains("shop_npcs")) {
            return;
        }
        List<ShopNpc> shopNpcs = new ArrayList<>();
        ConfigurationSection npcSection = config.getConfigurationSection("shop_npcs");
        if (npcSection != null) {
            String fallbackSkin = config.getString("shop_npcs.skin");
            String fallbackDisplayName = config.getString("shop_npcs.displayName");
            for (String key : npcSection.getKeys(false)) {
                if (key.equals("skin") || key.equals("displayName")) {
                    continue;
                }
                Location loc = ArenaLocationCodec.parseFor(config.getString("shop_npcs." + key + ".location"), targetWorld);
                if (loc == null) {
                    continue;
                }
                String skin = config.getString("shop_npcs." + key + ".skin", fallbackSkin);
                String displayName = config.getString("shop_npcs." + key + ".displayName", fallbackDisplayName);
                shopNpcs.add(new ShopNpc(loc, skin, displayName));
            }
        }
        if (!shopNpcs.isEmpty()) {
            arena.setShopNpcs(shopNpcs);
        }
    }

    void readGeneratorConfigs(final YamlConfiguration config, final Arena arena) {
        if (!config.contains("generator_config")) {
            return;
        }
        Map<String, GeneratorConfig> genConfigs = new HashMap<>();
        for (String type : config.getConfigurationSection("generator_config").getKeys(false)) {
            String path = "generator_config." + type;
            String matName = config.getString(path + ".material");
            Material mat = matName != null ? Material.matchMaterial(matName) : null;
            Map<Integer, Long> levels = new HashMap<>();
            if (config.contains(path + ".levels")) {
                for (String levelKey : config.getConfigurationSection(path + ".levels").getKeys(false)) {
                    levels.put(Integer.parseInt(levelKey), config.getLong(path + ".levels." + levelKey, 0L));
                }
            }
            if (mat != null && !levels.isEmpty()) {
                genConfigs.put(type, new GeneratorConfig(mat, levels));
            }
        }
        if (!genConfigs.isEmpty()) {
            arena.setGeneratorConfigs(genConfigs);
        }
    }

    void readLevelTimes(final YamlConfiguration config, final Arena arena) {
        if (!config.contains("level-times")) {
            return;
        }
        Map<Integer, Integer> levelTimes = new HashMap<>();
        for (String minuteKey : config.getConfigurationSection("level-times").getKeys(false)) {
            levelTimes.put(Integer.parseInt(minuteKey), config.getInt("level-times." + minuteKey, 1));
        }
        if (!levelTimes.isEmpty()) {
            arena.setLevelTimes(levelTimes);
        }
    }

    List<String> parseEnabledCommands(final Object raw) {
        return ArenaCommandParser.parse(raw);
    }

    void writeSpawnItems(final YamlConfiguration config, final Arena arena) {
        if (arena.getSpawnItems() != null && !arena.getSpawnItems().isEmpty()) {
            final List<String> spawnItemNames = new ArrayList<>();
            for (final Material material : arena.getSpawnItems()) {
                spawnItemNames.add(material.name());
            }
            config.set("spawn_item", spawnItemNames);
        } else {
            config.set("spawn_item", null);
        }
    }

    void writeShopNpcs(final YamlConfiguration config, final @Nullable YamlConfiguration disk, final Arena arena) {
        List<ShopNpc> shopNpcs = arena.getShopNpcs();
        if (shopNpcs != null && !shopNpcs.isEmpty()) {
            for (int i = 0; i < shopNpcs.size(); i++) {
                ShopNpc npc = shopNpcs.get(i);
                config.set("shop_npcs." + i + ".location", ArenaLocationCodec.serialize(npc.location()));
                if (npc.skin() != null) {
                    config.set("shop_npcs." + i + ".skin", npc.skin());
                }
                if (npc.displayName() != null) {
                    config.set("shop_npcs." + i + ".displayName", npc.displayName());
                }
            }
        } else if (disk != null && disk.contains("shop_npcs") && !this.sectionWorldLoaded(disk, "shop_npcs")) {
            this.copySection(disk, config, "shop_npcs");
        }
    }

    void writeGeneratorConfigs(final YamlConfiguration config, final Arena arena) {
        for (var entry : arena.getGeneratorConfigs().entrySet()) {
            String type = entry.getKey();
            GeneratorConfig gc = entry.getValue();
            config.set("generator_config." + type + ".material", gc.material().name());
            config.set("generator_config." + type + ".levels", null);
            for (var levelEntry : gc.levels().entrySet()) {
                config.set("generator_config." + type + ".levels." + levelEntry.getKey(), levelEntry.getValue());
            }
        }
    }

    void writeLevelTimes(final YamlConfiguration config, final Arena arena) {
        config.set("level-times", null);
        if (arena.getLevelTimes() != null) {
            for (var entry : arena.getLevelTimes().entrySet()) {
                config.set("level-times." + entry.getKey(), entry.getValue());
            }
        }
    }

    void writeTeams(final YamlConfiguration config, final @Nullable YamlConfiguration disk, final Arena arena) {
        config.set("teams", null);
        for (final ArenaTeam team : arena.getTeams()) {
            final String path = "teams." + team.getName();
            config.set(path + ".color", team.getColor());
            this.writeLocation(config, disk, path + ".spawn", team.getSpawn());
            if (team.getSpawnBlockData() != null) {
                config.set(path + ".spawn_block", team.getSpawnBlockData());
            }
            this.writeLocation(config, disk, path + ".bed", team.getBed());
            if (team.getBedFacing() != null) {
                config.set(path + ".bed_facing", team.getBedFacing());
            }
        }
    }

    void writeGenerators(final YamlConfiguration config, final @Nullable YamlConfiguration disk, final Arena arena) {
        final boolean hasResolvedGenerator = arena.getGenerators().stream()
                .anyMatch(gen -> gen.getLocation() != null);
        if (hasResolvedGenerator) {
            config.set("generators", null);
            for (final ArenaGenerator gen : arena.getGenerators()) {
                if (gen.getLocation() == null) {
                    continue;
                }
                final String path = "generators." + gen.getUniqueId().toString();
                config.set(path + ".type", gen.getType());
                config.set(path + ".location", ArenaLocationCodec.serialize(gen.getLocation()));
                if (gen.getTeam() != null) {
                    config.set(path + ".team", gen.getTeam());
                }
                if (gen.getOriginBlockData() != null) {
                    config.set(path + ".origin_block", gen.getOriginBlockData());
                }
                if (gen.getOriginBlockDataAbove() != null) {
                    config.set(path + ".origin_block_above", gen.getOriginBlockDataAbove());
                }
            }
        } else if (disk != null && disk.contains("generators") && !this.sectionWorldLoaded(disk, "generators")) {
            this.copySection(disk, config, "generators");
        }
    }

    /**
     * Grava uma localização, preservando o valor anterior quando ela está null
     * no cache (arena ainda sem mundo resolvido). Isso evita que um flush/save
     * com referências não resolvidas remova permanentemente as chaves do arquivo.
     */
    void writeLocation(final YamlConfiguration config, final @Nullable YamlConfiguration disk,
                       final String path, final @Nullable Location loc) {
        if (loc != null) {
            config.set(path, ArenaLocationCodec.serialize(loc));
            return;
        }
        if (disk != null && disk.contains(path)) {
            final String stored = disk.getString(path);
            if (stored != null && !stored.isBlank()) {
                config.set(path, stored);
                return;
            }
        }
        config.set(path, null);
    }

    /**
     * Verifica se alguma localização armazenada em uma seção do disco referencia
     * um mundo atualmente carregado.
     */
    boolean sectionWorldLoaded(final YamlConfiguration disk, final String section) {
        final ConfigurationSection cs = disk != null ? disk.getConfigurationSection(section) : null;
        if (cs == null) {
            return false;
        }
        for (final String key : cs.getKeys(true)) {
            final Object value = cs.get(key);
            if (!(value instanceof final String str) || !str.contains(",")) {
                continue;
            }
            if (Bukkit.getWorld(str.split(",", 2)[0]) != null) {
                return true;
            }
        }
        return false;
    }

    void copySection(final YamlConfiguration source, final YamlConfiguration target, final String section) {
        final ConfigurationSection cs = source.getConfigurationSection(section);
        if (cs == null) {
            return;
        }
        for (final String key : cs.getKeys(true)) {
            final Object value = source.get(section + "." + key);
            if (value instanceof ConfigurationSection) {
                continue;
            }
            target.set(section + "." + key, value);
        }
    }

    String serializeLocation(final Location loc) {
        return ArenaLocationCodec.serialize(loc);
    }

    @Nullable Location parseLocationFor(final String str, final @Nullable World targetWorld) {
        return ArenaLocationCodec.parseFor(str, targetWorld);
    }
}
