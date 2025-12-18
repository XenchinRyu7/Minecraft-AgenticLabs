package com.xenchinryu7.mcagenticlabs.memory;

import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class WorldKnowledge {
    private final AgentEntity agent;
    private final int scanRadius = 16;
    private Map<Block, Integer> nearbyBlocks;
    private List<Entity> nearbyEntities;
    private String biomeName;

    public WorldKnowledge(AgentEntity agent) {
        this.agent = agent;
        scan();
    }

    private void scan() {
        scanBiome();
        scanBlocks();
        scanEntities();
    }

    private void scanBiome() {
        Level level = agent.level();
        BlockPos pos = agent.blockPosition();
        
        Biome biome = level.getBiome(pos).value();
        Registry<Biome> biomeRegistry = level.registryAccess().lookup(Registries.BIOME).orElseThrow();
        var biomeKey = biomeRegistry.getKey(biome);
        
        if (biomeKey != null) {
            biomeName = biomeKey.getPath();
        } else {
            biomeName = "unknown";
        }
    }

    private void scanBlocks() {
        nearbyBlocks = new HashMap<>();
        Level level = agent.level();
        BlockPos agentPos = agent.blockPosition();
        
        for (int x = -scanRadius; x <= scanRadius; x += 2) {
            for (int y = -scanRadius; y <= scanRadius; y += 2) {
                for (int z = -scanRadius; z <= scanRadius; z += 2) {
                    BlockPos checkPos = agentPos.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    Block block = state.getBlock();
                    
                    if (block != Blocks.AIR && block != Blocks.CAVE_AIR && block != Blocks.VOID_AIR) {
                        nearbyBlocks.put(block, nearbyBlocks.getOrDefault(block, 0) + 1);
                    }
                }
            }
        }
    }

    private void scanEntities() {
        Level level = agent.level();
        AABB searchBox = agent.getBoundingBox().inflate(scanRadius);
        nearbyEntities = level.getEntities(agent, searchBox);
    }

    public String getBiomeName() {
        return biomeName;
    }

    public String getNearbyBlocksSummary() {
        if (nearbyBlocks.isEmpty()) {
            return "none";
        }
        
        List<Map.Entry<Block, Integer>> sorted = nearbyBlocks.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .toList();
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(", ");
            Map.Entry<Block, Integer> entry = sorted.get(i);
            sb.append(entry.getKey().getName().getString());
        }
        
        return sb.toString();
    }

    public String getNearbyEntitiesSummary() {
        if (nearbyEntities.isEmpty()) {
            return "none";
        }
        
        Map<String, Integer> entityCounts = new HashMap<>();
        for (Entity entity : nearbyEntities) {
            String name = entity.getType().toString();
            entityCounts.put(name, entityCounts.getOrDefault(name, 0) + 1);
        }
        
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : entityCounts.entrySet()) {
            if (count > 0) sb.append(", ");
            sb.append(entry.getValue()).append(" ").append(entry.getKey());
            count++;
            if (count >= 5) break;
        }
        
        return sb.toString();
    }

    public Map<Block, Integer> getNearbyBlocks() {
        return nearbyBlocks;
    }

    public List<Entity> getNearbyEntities() {
        return nearbyEntities;
    }

    public String getNearbyPlayerNames() {
        List<String> playerNames = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player player) {
                playerNames.add(player.getName().getString());
            }
        }
        
        if (playerNames.isEmpty()) {
            return "none";
        }
        
        return String.join(", ", playerNames);
    }
}

