package com.xenchinryu7.mcagenticlabs.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;

/**
 * Client-side initialization for Agent AI Mod
 */
public class AgentsClient implements ClientModInitializer {

    private static final ResourceLocation agent_TEXTURE = ResourceLocation.parse("mc-agenticlabs:textures/entity/player/agent.png");

    @Override
    public void onInitializeClient() {
        // Initialize key bindings
        KeyBindings.init();

        // Register entity renderer
        EntityRendererRegistry.register(AgentsMod.AGENTS_ENTITY, (EntityRendererProvider.Context context) -> new AgentRenderer(context));

        // Register client tick event for GUI and keybindings
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle key presses
            if (KeyBindings.TOGGLE_GUI != null && KeyBindings.TOGGLE_GUI.consumeClick()) {
                AgentGUI.toggle();
            }

            // Disable narrator if needed
            if (client.options != null && !client.options.narrator().get().equals(net.minecraft.client.NarratorStatus.OFF)) {
                client.options.narrator().set(net.minecraft.client.NarratorStatus.OFF);
                client.options.save();
            }
        });
    }

    private static class AgentRenderer extends LivingEntityRenderer<AgentEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
        public AgentRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        }

        @Override
        public HumanoidRenderState createRenderState() {
            return new HumanoidRenderState();
        }

        @Override
        public ResourceLocation getTextureLocation(HumanoidRenderState state) {
            return agent_TEXTURE;
        }
    }
}
