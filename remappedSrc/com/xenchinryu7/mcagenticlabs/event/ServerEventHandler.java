package com.xenchinryu7.mcagenticlabs.event;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import com.xenchinryu7.mcagenticlabs.entity.agentManager;
import com.xenchinryu7.mcagenticlabs.memory.StructureRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class ServerEventHandler {
    private static boolean agentsSpawned = false;

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel level = (ServerLevel) player.level();
            agentManager manager = AgentsMod.getAgentsManager();

            if (!agentsSpawned && manager != null) {
                manager.clearAllAgents();

                // Clear structure registry for fresh spatial awareness
                StructureRegistry.clear();

                // Then, remove ALL agentEntity instances from the world (including ones loaded from NBT)
                int removedCount = 0;
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof agentEntity) {
                        entity.discard();
                        removedCount++;
                    }
                }

                Vec3 playerPos = player.position();
                Vec3 lookVec = player.getLookAngle();

                String[] names = {"Agent", "Alex", "Bob", "Charlie"};

                for (int i = 0; i < 4; i++) {
                    double offsetX = lookVec.x * 5 + (lookVec.z * (i - 1.5) * 2);
                    double offsetZ = lookVec.z * 5 + (-lookVec.x * (i - 1.5) * 2);

                    Vec3 spawnPos = new Vec3(
                        playerPos.x + offsetX,
                        playerPos.y,
                        playerPos.z + offsetZ
                    );

                    AgentEntity agent = manager.spawnAgent(level, spawnPos, names[i]);
                    if (Agent != null) {
                        // Agent spawned successfully
                    }
                }

                agentsSpawned = true;
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            agentsSpawned = false;
        });
    }
}

