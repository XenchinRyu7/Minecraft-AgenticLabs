package com.xenchinryu7.mcagenticlabs.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Side-mounted GUI panel for Agent agent interaction.
 * Inspired by Cursor's composer - slides in/out from the right side.
 * Now with scrollable message history!
 */
public class AgentGUI {
    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_PADDING = 6;
    private static final int ANIMATION_SPEED = 20;
    private static final int MESSAGE_HEIGHT = 12;
    private static final int MAX_MESSAGES = 500;
    private static final int BUTTON_WIDTH = 40;
    private static final int INPUT_HEIGHT = 20;
    
    private static boolean isOpen = false;
    private static float slideOffset = PANEL_WIDTH; // Start fully hidden
    public static EditBox inputBox;
    public static Button sendButton; // Use native Button widget
    private static List<String> commandHistory = new ArrayList<>();
    private static int historyIndex = -1;
    
    // Message history and scrolling
    private static List<ChatMessage> messages = new ArrayList<>();
    private static int scrollOffset = 0;
    private static int maxScroll = 0;
    private static final int BACKGROUND_COLOR = 0x15202020; // Ultra transparent (15 = ~8% opacity)
    private static final int BORDER_COLOR = 0x40404040; // More transparent border
    private static final int HEADER_COLOR = 0x25252525; // More transparent header (~15% opacity)
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    
    // Message bubble colors
    private static final int USER_BUBBLE_COLOR = 0xC04CAF50; // Green bubble for user
    private static final int agent_BUBBLE_COLOR = 0xC02196F3; // Blue bubble for agent
    private static final int SYSTEM_BUBBLE_COLOR = 0xC0FF9800; // Orange bubble for system

    private static class ChatMessage {
        String sender; // "You", "agent", "Alex", "System", etc.
        String text;
        int bubbleColor;
        boolean isUser; // true if message from user
        
        ChatMessage(String sender, String text, int bubbleColor, boolean isUser) {
            this.sender = sender;
            this.text = text;
            this.bubbleColor = bubbleColor;
            this.isUser = isUser;
        }
    }

    public static void toggle() {
        isOpen = !isOpen;
        
        Minecraft mc = Minecraft.getInstance();
        
        if (isOpen) {
            initializeInputBox();
            mc.setScreen(new agentOverlayScreen());
            if (inputBox != null) {
                inputBox.setFocused(true);
            }
        } else {
            close();
            if (mc.screen instanceof agentOverlayScreen) {
                mc.setScreen(null);
            }
        }
    }

    public static void close() {
        isOpen = false;
        if (inputBox != null) {
            inputBox = null;
        }
    }

    public static boolean isOpen() {
        return isOpen;
    }

    private static void initializeInputBox() {
        Minecraft mc = Minecraft.getInstance();
        if (inputBox == null) {
            // Reduce width to make room for Send button on the right
            inputBox = new EditBox(mc.font, 0, 0, PANEL_WIDTH - BUTTON_WIDTH - 30, INPUT_HEIGHT, 
                Component.literal("Command"));
            inputBox.setMaxLength(256);
            inputBox.setHint(Component.literal("Tell Agent what to do..."));
            inputBox.setFocused(true);
        }
        
        // Initialize Send button if needed
        if (sendButton == null) {
            sendButton = Button.builder(Component.literal("Send"), button -> {
                // Button click handler - this runs when button is clicked!
                if (inputBox != null) {
                    String command = inputBox.getValue().trim();
                    if (!command.isEmpty()) {
                        sendCommand(command);
                        inputBox.setValue("");
                        addSystemMessage("Command sent!");
                    } else {
                        addSystemMessage("Cannot send empty command!");
                    }
                }
            })
            .bounds(0, 0, BUTTON_WIDTH, INPUT_HEIGHT)
            .build();
        }
    }

    /**
     * Add a message to the chat history
     */
    public static void addMessage(String sender, String text, int bubbleColor, boolean isUser) {
        messages.add(new ChatMessage(sender, text, bubbleColor, isUser));
        if (messages.size() > MAX_MESSAGES) {
            messages.remove(0);
        }
        // Auto-scroll to bottom on new message
        scrollOffset = 0;
    }

    /**
     * Add a user command to the history
     */
    public static void addUserMessage(String text) {
        addMessage("You", text, USER_BUBBLE_COLOR, true);
    }

    /**
     * Add a Agent response to the history
     */
    public static void addagentMessage(String agentName, String text) {
        addMessage(agentName, text, agent_BUBBLE_COLOR, false);
    }

    /**
     * Add a system message to the history
     */
    public static void addSystemMessage(String text) {
        addMessage("System", text, SYSTEM_BUBBLE_COLOR, false);
    }

    // @SubscribeEvent
    // public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
    /**
     * Simple word wrap for text
     */
    private static String wrapText(net.minecraft.client.gui.Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        // Simple truncation for now
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            result.append(text.charAt(i));
            if (font.width(result.toString() + "...") >= maxWidth) {
                return result.substring(0, result.length() - 3) + "...";
            }
        }
        return result.toString();
    }

    public static void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isOpen) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Animate slide
        if (slideOffset > 0) {
            slideOffset = Math.max(0, slideOffset - ANIMATION_SPEED);
        }

        int panelX = screenWidth - PANEL_WIDTH + (int)slideOffset;

        // Draw background
        graphics.fill(panelX, 0, screenWidth, screenHeight, BACKGROUND_COLOR);

        // Draw header
        graphics.fill(panelX, 0, screenWidth, 20, HEADER_COLOR);
        graphics.drawString(mc.font, "Agent AI", panelX + PANEL_PADDING, 6, TEXT_COLOR, false);

        // Draw messages
        int y = 25;
        for (int i = Math.max(0, messages.size() - 1 - scrollOffset); i < messages.size() && y < screenHeight - 30; i++) {
            ChatMessage msg = messages.get(i);
            String wrapped = wrapText(mc.font, msg.sender + ": " + msg.text, PANEL_WIDTH - 20);
            graphics.drawString(mc.font, wrapped, panelX + PANEL_PADDING, y, TEXT_COLOR, false);
            y += MESSAGE_HEIGHT;
        }

        // Draw input box and Send button
        if (inputBox != null) {
            inputBox.setX(panelX + PANEL_PADDING);
            inputBox.setY(screenHeight - 25);
            inputBox.setWidth(PANEL_WIDTH - BUTTON_WIDTH - 30);
            inputBox.render(graphics, mouseX, mouseY, partialTick);
            
            // Position Send button next to input box
            if (sendButton != null) {
                sendButton.setX(panelX + PANEL_PADDING + inputBox.getWidth() + 4);
                sendButton.setY(screenHeight - 25);
                sendButton.setWidth(BUTTON_WIDTH);
                sendButton.setHeight(INPUT_HEIGHT);
                sendButton.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    public static boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (!isOpen || inputBox == null) return false;

        Minecraft mc = Minecraft.getInstance();
        
        // Escape key - close panel
        if (keyCode == 256) { // ESC
            toggle();
            return true;
        }
        
        // Enter key - send command
        if (keyCode == 257) {
            String command = inputBox.getValue().trim();
            if (!command.isEmpty()) {
                sendCommand(command);
                inputBox.setValue("");
                historyIndex = -1;
            }
            return true;
        }

        // Arrow up - previous command
        if (keyCode == 265 && !commandHistory.isEmpty()) { // UP
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputBox.setValue(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
            return true;
        }

        // Arrow down - next command
        if (keyCode == 264) { // DOWN
            if (historyIndex > 0) {
                historyIndex--;
                inputBox.setValue(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else if (historyIndex == 0) {
                historyIndex = -1;
                inputBox.setValue("");
            }
            return true;
        }

        // Backspace, Delete, Home, End, Left, Right - handled by EditBox widget automatically
        if (keyCode == 259 || keyCode == 261 || keyCode == 268 || keyCode == 269 || 
            keyCode == 263 || keyCode == 262) {
            return true;
        }

        return true; // Consume all keys to prevent game controls
    }

    public static boolean handleCharTyped(char codePoint, int modifiers) {
        // Character input is handled automatically by the EditBox widget
        // when it's properly added to the Screen (done in agentOverlayScreen.init())
        if (isOpen && inputBox != null && inputBox.isFocused()) {
            return true; // Consumed
        }
        return false;
    }

    public static void handleMouseClick(double mouseX, double mouseY, int button) {
        if (!isOpen) return;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Handle input box focus (Button widget handles its own clicks)
        if (inputBox != null) {
            int inputAreaY = screenHeight - 80;
            if (mouseY >= inputAreaY + 25 && mouseY <= inputAreaY + 45) {
                inputBox.setFocused(true);
            } else {
                inputBox.setFocused(false);
            }
        }
    }

    public static void handleMouseScroll(double scrollDelta) {
        if (!isOpen) return;
        
        int scrollAmount = (int)(scrollDelta * 3 * MESSAGE_HEIGHT);
        scrollOffset -= scrollAmount;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    /**
     * Public wrapper for sending commands from the overlay screen
     */
    public static void sendCommandFromScreen(String command) {
        sendCommand(command);
    }

    private static void sendCommand(String command) {
        Minecraft mc = Minecraft.getInstance();
        
        commandHistory.add(command);
        if (commandHistory.size() > 50) {
            commandHistory.remove(0);
        }
        
        addUserMessage(command);

        if (command.toLowerCase().startsWith("spawn ")) {
            String name = command.substring(6).trim();
            if (name.isEmpty()) name = "agent";
            if (mc.player != null) {
                mc.player.connection.sendCommand("agents spawn " + name);
                addSystemMessage("Spawning Agent: " + name);
            }
            return;
        }

        List<String> targetAgents = parseTargetAgents(command);
        
        // Strip "all agents" or "everyone" prefix from command if present
        String actualCommand = command;
        String lowerCommand = command.toLowerCase();
        if (lowerCommand.startsWith("all agents ")) {
            actualCommand = command.substring(11).trim(); // Remove "all agents "
        } else if (lowerCommand.startsWith("all ")) {
            actualCommand = command.substring(4).trim(); // Remove "all "
        } else if (lowerCommand.startsWith("everyone ")) {
            actualCommand = command.substring(9).trim(); // Remove "everyone "
        } else if (lowerCommand.startsWith("everybody ")) {
            actualCommand = command.substring(10).trim(); // Remove "everybody "
        }
        
        if (targetAgents.isEmpty()) {
            var agents = AgentsMod.getAgentsManager().getAllAgents();
            if (!agents.isEmpty()) {
                targetAgents.add(agents.iterator().next().getAgentName());
            } else {
                // No agents available
                addSystemMessage("No agents found! Use 'spawn <name>' to create one.");
                return;
            }
        }

        // Send command to all targeted agents
        if (mc.player != null) {
            for (String agentName : targetAgents) {
                mc.player.connection.sendCommand("agents tell " + agentName + " " + actualCommand);
            }
            
            if (targetAgents.size() > 1) {
                addSystemMessage("→ " + String.join(", ", targetAgents) + ": " + actualCommand);
            } else {
                addSystemMessage("→ " + targetAgents.get(0) + ": " + actualCommand);
            }
        }
    }
    
    private static List<String> parseTargetAgents(String command) {
        List<String> targets = new ArrayList<>();
        String commandLower = command.toLowerCase();
        
        if (commandLower.startsWith("all agents ") || commandLower.startsWith("all ") || 
            commandLower.startsWith("everyone ") || commandLower.startsWith("everybody ")) {
            var allagents = AgentsMod.getAgentsManager().getAllAgents();
            for (AgentEntity agent : allagents) {
                targets.add(agent.getAgentName());
            }
            return targets;
        }
        
        var allagents = AgentsMod.getAgentsManager().getAllAgents();
        List<String> availableNames = new ArrayList<>();
        for (AgentEntity agent : allagents) {
            availableNames.add(agent.getAgentName().toLowerCase());
        }
        
        String[] parts = command.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            String firstWord = trimmed.split(" ")[0].toLowerCase();
            
            if (availableNames.contains(firstWord)) {
                for (AgentEntity agent : allagents) {
                    if (agent.getAgentName().equalsIgnoreCase(firstWord)) {
                        targets.add(agent.getAgentName());
                        break;
                    }
                }
            }
        }
        
        return targets;
    }

    public static void tick() {
        if (isOpen && inputBox != null) {
            // EditBox doesn't have tick() in 1.21.10
            // Auto-focus input box when panel is open
            if (!inputBox.isFocused()) {
                inputBox.setFocused(true);
            }
        }
    }
}
