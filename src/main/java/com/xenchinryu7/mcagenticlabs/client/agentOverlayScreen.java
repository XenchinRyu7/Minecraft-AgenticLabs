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
    protected void init() {
        super.init();
        if (AgentGUI.inputBox != null) {
            this.addWidget(AgentGUI.inputBox);
            this.setFocused(AgentGUI.inputBox);
        }
        // Add Send button widget - it will handle clicks automatically!
        if (AgentGUI.sendButton != null) {
            this.addRenderableWidget(AgentGUI.sendButton);
        }
    }

    public boolean isPauseScreen() {
        return false; // Don't pause the game
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render the blur/darkening background first
        super.render(graphics, mouseX, mouseY, partialTick);
        // Then render the AgentGUI on top
        AgentGUI.render(graphics, mouseX, mouseY, partialTick);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // K key to close (only if no modifiers pressed)
        if (keyCode == 75 && modifiers == 0) { // K
            AgentGUI.close();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }
        
        // CRITICAL: Check Enter key FIRST before widget consumes it
        // Enter key (257 or GLFW_KEY_ENTER) - send command
        if (keyCode == 257 || keyCode == 335) { // ENTER or NUMPAD_ENTER
            if (AgentGUI.inputBox != null && AgentGUI.inputBox.isFocused()) {
                String command = AgentGUI.inputBox.getValue().trim();
                if (!command.isEmpty()) {
                    AgentGUI.sendCommandFromScreen(command);
                    AgentGUI.inputBox.setValue("");
                    return true;
                }
            }
        }
        
        // Delegate other keys to AgentGUI
        return AgentGUI.handleKeyPress(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        // Pass character input to AgentGUI
        return AgentGUI.handleCharTyped(codePoint, modifiers);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Button widget handles its own clicks automatically via addRenderableWidget()
        // Just handle input box focus
        AgentGUI.handleMouseClick(mouseX, mouseY, button);
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        AgentGUI.handleMouseScroll(scrollDelta);
        return true;
    }

    public void removed() {
        // Clean up when screen is closed
        // No need to call toggle() here as it's handled in keyPressed
    }
}

