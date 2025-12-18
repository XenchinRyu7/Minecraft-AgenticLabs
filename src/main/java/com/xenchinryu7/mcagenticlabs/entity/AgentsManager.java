package com.xenchinryu7.mcagenticlabs.entity;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.config.AgentConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AgentsManager {
    private final Map<String, AgentEntity> activeAgents;
    private final Map<UUID, AgentEntity> agentsByUUID;

    public AgentsManager() {
        this.activeAgents = new ConcurrentHashMap<>();
        this.agentsByUUID = new ConcurrentHashMap<>();
    }

    public AgentEntity spawnAgent(ServerLevel level, Vec3 position, String name) {
        AgentsMod.LOGGER.info("Current active Agents: {}", activeAgents.size());

        if (activeAgents.containsKey(name)) {
            AgentsMod.LOGGER.warn("Agent name '{}' already exists", name);
            return null;
        }
        int maxAgents = AgentConfig.MAX_ACTIVE_AGENTS.get();
        if (activeAgents.size() >= maxAgents) {
            AgentsMod.LOGGER.warn("Max Agent limit reached: {}", maxAgents);
            return null;
        }
        AgentEntity agent;
        try {
            AgentsMod.LOGGER.info("EntityType: {}", AgentsMod.AGENTS_ENTITY);
            agent = new AgentEntity(AgentsMod.AGENTS_ENTITY, level);
        } catch (Throwable e) {
            AgentsMod.LOGGER.error("Failed to create Agent entity", e);
            AgentsMod.LOGGER.error("Exception class: {}", e.getClass().getName());
            AgentsMod.LOGGER.error("Exception message: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        try {            agent.setAgentName(name);            agent.setPos(position.x, position.y, position.z);            boolean added = level.addFreshEntity(agent);            if (added) {
                activeAgents.put(name, agent);
                agentsByUUID.put(agent.getUUID(), agent);
                AgentsMod.LOGGER.info("Successfully spawned agent: {} with UUID {} at {}", name, agent.getUUID(), position);                return agent;
            } else {
                AgentsMod.LOGGER.error("Failed to add Agent entity to world (addFreshEntity returned false)");
                AgentsMod.LOGGER.error("=== SPAWN ATTEMPT FAILED ===");
            }
        } catch (Throwable e) {
            AgentsMod.LOGGER.error("Exception during spawn setup", e);
            AgentsMod.LOGGER.error("=== SPAWN ATTEMPT FAILED WITH EXCEPTION ===");
            e.printStackTrace();
        }

        return null;
    }

    public AgentEntity getAgentByName(String name) {
        return activeAgents.get(name);
    }

    public AgentEntity getAgentByUUID(UUID uuid) {
        return agentsByUUID.get(uuid);
    }

    public boolean removeAgentByName(String name) {
        AgentEntity agent = activeAgents.remove(name);
        if (agent != null) {
            agentsByUUID.remove(agent.getUUID());
            agent.discard();            return true;
        }
        return false;
    }

    public void clearAllAgents() {
        AgentsMod.LOGGER.info("Clearing {} Agent entities", activeAgents.size());
        for (AgentEntity agent : activeAgents.values()) {
            agent.discard();
        }
        activeAgents.clear();
        agentsByUUID.clear();    }

    public Collection<AgentEntity> getAllAgents() {
        return Collections.unmodifiableCollection(activeAgents.values());
    }

    public List<String> getAgentNames() {
        return new ArrayList<>(activeAgents.keySet());
    }

    public int getActiveCount() {
        return activeAgents.size();
    }

    public void tick(ServerLevel level) {
        // Clean up dead or removed Agents
        Iterator<Map.Entry<String, AgentEntity>> iterator = activeAgents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AgentEntity> entry = iterator.next();
            AgentEntity agent = entry.getValue();
            
            if (!agent.isAlive() || agent.isRemoved()) {
                iterator.remove();
                agentsByUUID.remove(agent.getUUID());
                AgentsMod.LOGGER.info("Cleaned up agent: {}", entry.getKey());
            }
        }
    }
}

