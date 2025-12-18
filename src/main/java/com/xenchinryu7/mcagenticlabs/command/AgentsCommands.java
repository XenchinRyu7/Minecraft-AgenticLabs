package com.xenchinryu7.mcagenticlabs.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import com.xenchinryu7.mcagenticlabs.entity.AgentsManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public class AgentsCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("agents")
            .then(Commands.literal("spawn")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(AgentsCommands::spawnAgent)))
            .then(Commands.literal("remove")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(AgentsCommands::removeAgent)))
            .then(Commands.literal("list")
                .executes(AgentsCommands::listAgents))
            .then(Commands.literal("stop")
                .then(Commands.argument("name", StringArgumentType.string())
                    .executes(AgentsCommands::stopAgent)))
            .then(Commands.literal("tell")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("command", StringArgumentType.greedyString())
                        .executes(AgentsCommands::tellAgent))))
        );
    }

    private static int spawnAgent(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        ServerLevel serverLevel = source.getLevel();
        if (serverLevel == null) {
            source.sendFailure(Component.literal("Command must be run on server"));
            return 0;
        }

        AgentsManager manager = AgentsMod.getAgentsManager();
        
        Vec3 sourcePos = source.getPosition();
        if (source.getEntity() != null) {
            Vec3 lookVec = source.getEntity().getLookAngle();
            sourcePos = sourcePos.add(lookVec.x * 3, 0, lookVec.z * 3);
        } else {
            sourcePos = sourcePos.add(3, 0, 0);
        }
        Vec3 spawnPos = sourcePos;
        
        AgentEntity agent = manager.spawnAgent(serverLevel, spawnPos, name);
        if (agent != null) {
            source.sendSuccess(() -> Component.literal("Spawned Agent: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Failed to spawn Agent. Name may already exist or max limit reached."));
            return 0;
        }
    }

    private static int removeAgent(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        AgentsManager manager = AgentsMod.getAgentsManager();
        if (manager.removeAgentByName(name)) {
            source.sendSuccess(() -> Component.literal("Removed Agent: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Agent not found: " + name));
            return 0;
        }
    }

    private static int listAgents(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        AgentsManager manager = AgentsMod.getAgentsManager();
        
        var names = manager.getAgentNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active agents"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Active Agents (" + names.size() + "): " + String.join(", ", names)), false);
        }
        return 1;
    }

    private static int stopAgent(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        CommandSourceStack source = context.getSource();
        
        AgentsManager manager = AgentsMod.getAgentsManager();
        AgentEntity agent = manager.getAgentByName(name);

        if (agent != null) {
            agent.getActionExecutor().stopCurrentAction();
            agent.getMemory().clearTaskQueue();
            source.sendSuccess(() -> Component.literal("Stopped Agent: " + name), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Agent not found: " + name));
            return 0;
        }
    }

    private static int tellAgent(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        String command = StringArgumentType.getString(context, "command");
        CommandSourceStack source = context.getSource();
        
        AgentsManager manager = AgentsMod.getAgentsManager();
        AgentEntity agent = manager.getAgentByName(name);

        if (agent != null) {
            // Disabled command feedback message
            // source.sendSuccess(() -> Component.literal("Instructing " + name + ": " + command), true);

            new Thread(() -> {
                agent.getActionExecutor().processNaturalLanguageCommand(command);
            }).start();
            
            return 1;
        } else {
            source.sendFailure(Component.literal("Agent not found: " + name));
            return 0;
        }
    }
}

