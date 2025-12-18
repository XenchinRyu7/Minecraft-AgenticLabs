package com.xenchinryu7.mcagenticlabs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;

/**
 * Client-side initialization for Agent AI Mod
 */
public class AgentsClient implements ClientModInitializer {

    private static final ResourceLocation agent_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/agent.png");

    @Override
    public void onInitializeClient() {
        // Initialize key bindings
        KeyBindings.init();

        // Register entity renderer
        EntityRendererRegistry.register(AgentsMod.AGENTS_ENTITY, context ->
            new HumanoidMobRenderer<agentEntity, PlayerModel<agentEntity>>(
                context,
                new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false),
                0.5F
            ) {
                @Override
                public ResourceLocation getTextureLocation(agentEntity entity) {
                    return agent_TEXTURE;
                }
            }
        );

        // Register client tick event for GUI and keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle key presses
            if (KeyBindings.TOGGLE_GUI != null && KeyBindings.TOGGLE_GUI.consumeClick()) {
                agentGUI.toggle();
            }

            // Disable narrator if needed
            if (client.options != null && !client.options.narrator().get().equals(net.minecraft.client.NarratorStatus.OFF)) {
                client.options.narrator().set(net.minecraft.client.NarratorStatus.OFF);
                client.options.save();
            }
        });
    }
}
