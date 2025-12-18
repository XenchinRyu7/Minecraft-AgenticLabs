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

    @Override
    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Don't render anything - the agentGUI renders via overlay
        // This screen is just to capture input
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // K key to close
        if (keyCode == 75 && !hasShiftDown() && !hasControlDown() && !hasAltDown()) { // K
            agentGUI.toggle();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }
        
        return agentGUI.handleKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // Pass character input to agentGUI
        return agentGUI.handleCharTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        agentGUI.handleMouseClick(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        agentGUI.handleMouseScroll(scrollDelta);
        return true;
    }

    @Override
    public void removed() {
        // Clean up when screen is closed
        if (agentGUI.isOpen()) {
            agentGUI.toggle();
        }
    }
}

