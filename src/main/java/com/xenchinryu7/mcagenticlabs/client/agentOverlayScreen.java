package com.xenchinryu7.mcagenticlabs.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Invisible overlay screen that captures input for the Agent GUI
 * This prevents game controls from activating while typing
 */
public class agentOverlayScreen extends Screen {
    
    public agentOverlayScreen() {
        super(Component.literal("Agent AI"));
    }

    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Don't render anything - the AgentGUI renders via overlay
        // This screen is just to capture input
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // K key to close (only if no modifiers pressed)
        if (keyCode == 75 && modifiers == 0) { // K
            AgentGUI.toggle();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }
        
        return AgentGUI.handleKeyPress(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        // Pass character input to AgentGUI
        return AgentGUI.handleCharTyped(codePoint, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        AgentGUI.handleMouseClick(mouseX, mouseY, button);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        AgentGUI.handleMouseScroll(scrollDelta);
        return true;
    }

    public void removed() {
        // Clean up when screen is closed
        if (AgentGUI.isOpen()) {
            AgentGUI.toggle();
        }
    }
}

