package com.xenchinryu7.mcagenticlabs.action;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.action.actions.*;
import com.xenchinryu7.mcagenticlabs.ai.ResponseParser;
import com.xenchinryu7.mcagenticlabs.ai.TaskPlanner;
import com.xenchinryu7.mcagenticlabs.config.AgentConfig;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;

import java.util.LinkedList;
import java.util.Queue;

public class ActionExecutor {
    private final AgentEntity agent;
    private TaskPlanner taskPlanner;  // Lazy-initialized to avoid loading dependencies on entity creation
    private final Queue<Task> taskQueue;
    
    private BaseAction currentAction;
    private String currentGoal;
    private int ticksSinceLastAction;
    private BaseAction idleFollowAction;  // Follow player when idle

    public ActionExecutor(AgentEntity agent) {
        this.agent = agent;
        this.taskPlanner = null;  // Will be initialized when first needed
        this.taskQueue = new LinkedList<>();
        this.ticksSinceLastAction = 0;
        this.idleFollowAction = null;
    }
    
    private TaskPlanner getTaskPlanner() {
        if (taskPlanner == null) {
            AgentsMod.LOGGER.info("Initializing TaskPlanner for Agent '{}'", agent.getAgentName());
            taskPlanner = new TaskPlanner();
        }
        return taskPlanner;
    }

    public void processNaturalLanguageCommand(String command) {
        AgentsMod.LOGGER.info("Agent '{}' processing command: {}", agent.getAgentName(), command);
        
        if (currentAction != null) {            currentAction.cancel();
            currentAction = null;
        }
        
        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
        
        try {
            ResponseParser.ParsedResponse response = getTaskPlanner().planTasks(agent, command);
            
            if (response == null) {
                sendToGUI(agent.getAgentName(), "I couldn't understand that command.");
                return;
            }

            currentGoal = response.getPlan();
            agent.getMemory().setCurrentGoal(currentGoal);
            
            taskQueue.clear();
            taskQueue.addAll(response.getTasks());
            
            // Send response to GUI pane only
            if (AgentConfig.ENABLE_CHAT_RESPONSES.get()) {
                sendToGUI(agent.getAgentName(), "Okay! " + currentGoal);
            }
        } catch (NoClassDefFoundError e) {
            AgentsMod.LOGGER.error("Failed to initialize AI components", e);
            sendToGUI(agent.getAgentName(), "Sorry, I'm having trouble with my AI systems!");
        }
        
        AgentsMod.LOGGER.info("Agent '{}' queued {} tasks", agent.getAgentName(), taskQueue.size());
    }
    
    /**
     * Send a message to the GUI pane (client-side only, no chat spam)
     */
    private void sendToGUI(String agentName, String message) {
        if (agent.level().isClientSide) {
            com.xenchinryu7.mcagenticlabs.client.agentGUI.addagentMessage(agentName, message);
        }
    }

    public void tick() {
        ticksSinceLastAction++;
        
        if (currentAction != null) {
            if (currentAction.isComplete()) {
                ActionResult result = currentAction.getResult();
                AgentsMod.LOGGER.info("Agent '{}' - Action completed: {} (Success: {})", 
                    agent.getAgentName(), result.getMessage(), result.isSuccess());
                
                agent.getMemory().addAction(currentAction.getDescription());
                
                if (!result.isSuccess() && result.requiresReplanning()) {
                    // Action failed, need to replan
                    if (AgentConfig.ENABLE_CHAT_RESPONSES.get()) {
                        sendToGUI(agent.getAgentName(), "Problem: " + result.getMessage());
                    }
                }
                
                currentAction = null;
            } else {
                if (ticksSinceLastAction % 100 == 0) {
                    AgentsMod.LOGGER.info("Agent '{}' - Ticking action: {}", 
                        agent.getAgentName(), currentAction.getDescription());
                }
                currentAction.tick();
                return;
            }
        }

        if (ticksSinceLastAction >= AgentConfig.ACTION_TICK_DELAY.get()) {
            if (!taskQueue.isEmpty()) {
                Task nextTask = taskQueue.poll();
                executeTask(nextTask);
                ticksSinceLastAction = 0;
                return;
            }
        }
        
        // When completely idle (no tasks, no goal), follow nearest player
        if (taskQueue.isEmpty() && currentAction == null && currentGoal == null) {
            if (idleFollowAction == null) {
                idleFollowAction = new IdleFollowAction(agent);
                idleFollowAction.start();
            } else if (idleFollowAction.isComplete()) {
                // Restart idle following if it stopped
                idleFollowAction = new IdleFollowAction(agent);
                idleFollowAction.start();
            } else {
                // Continue idle following
                idleFollowAction.tick();
            }
        } else if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
    }

    private void executeTask(Task task) {
        AgentsMod.LOGGER.info("Agent '{}' executing task: {} (action type: {})", 
            agent.getAgentName(), task, task.getAction());
        
        currentAction = createAction(task);
        
        if (currentAction == null) {
            AgentsMod.LOGGER.error("FAILED to create action for task: {}", task);
            return;
        }

        AgentsMod.LOGGER.info("Created action: {} - starting now...", currentAction.getClass().getSimpleName());
        currentAction.start();
        AgentsMod.LOGGER.info("Action started! Is complete: {}", currentAction.isComplete());
    }

    private BaseAction createAction(Task task) {
        return switch (task.getAction()) {
            case "pathfind" -> new PathfindAction(agent, task);
            case "mine" -> new MineBlockAction(agent, task);
            case "place" -> new PlaceBlockAction(agent, task);
            case "craft" -> new CraftItemAction(agent, task);
            case "attack" -> new CombatAction(agent, task);
            case "follow" -> new FollowPlayerAction(agent, task);
            case "gather" -> new GatherResourceAction(agent, task);
            case "build" -> new BuildStructureAction(agent, task);
            default -> {
                AgentsMod.LOGGER.warn("Unknown action type: {}", task.getAction());
                yield null;
            }
        };
    }

    public void stopCurrentAction() {
        if (currentAction != null) {
            currentAction.cancel();
            currentAction = null;
        }
        if (idleFollowAction != null) {
            idleFollowAction.cancel();
            idleFollowAction = null;
        }
        taskQueue.clear();
        currentGoal = null;
    }

    public boolean isExecuting() {
        return currentAction != null || !taskQueue.isEmpty();
    }

    public String getCurrentGoal() {
        return currentGoal;
    }
}

