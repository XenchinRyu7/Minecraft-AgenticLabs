package com.xenchinryu7.mcagenticlabs;

import com.mojang.logging.LogUtils;
import com.xenchinryu7.mcagenticlabs.command.AgentsCommands;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import com.xenchinryu7.mcagenticlabs.entity.AgentsManager;
import com.xenchinryu7.mcagenticlabs.event.ServerEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;

public class AgentsMod implements ModInitializer {
    public static final String MODID = "mc-agenticlabs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final EntityType<agentEntity> AGENTS_ENTITY = EntityType.Builder.of(agentEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.8F)
        .clientTrackingRange(10)
        .build("agents");

    private static AgentsManager agentsManager;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Agent AI Mod...");

        // Register entity
        Registry.register(BuiltInRegistries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(MODID, "agents"), AGENTS_ENTITY);

        // Register entity attributes
        FabricDefaultAttributeRegistry.register(AGENTS_ENTITY, agentEntity.createAttributes().build());

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AgentsCommands.register(dispatcher);
        });

        // Initialize server events
        ServerEventHandler.init();

        // Initialize Agents manager
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            agentsManager = new AgentsManager();
        });

        LOGGER.info("Agent AI Mod initialized!");
    }

    public static AgentsManager getAgentsManager() {
        return agentsManager;
    }
}

