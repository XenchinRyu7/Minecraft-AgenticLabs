package com.xenchinryu7.mcagenticlabs.action.actions;

import com.xenchinryu7.mcagenticlabs.action.ActionResult;
import com.xenchinryu7.mcagenticlabs.action.Task;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class FollowPlayerAction extends BaseAction {
    private String playerName;
    private Player targetPlayer;
    private int ticksRunning;
    private static final int MAX_TICKS = 6000; // 5 minutes

    public FollowPlayerAction(AgentEntity agent, Task task) {
        super(agent, task);
    }

    @Override
    protected void onStart() {
        playerName = task.getStringParameter("player");
        ticksRunning = 0;
        
        findPlayer();
        
        if (targetPlayer == null) {
            result = ActionResult.failure("Player not found: " + playerName);
        }
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.success("Stopped following");
            return;
        }
        
        if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isRemoved()) {
            findPlayer();
            if (targetPlayer == null) {
                result = ActionResult.failure("Lost track of player");
                return;
            }
        }
        
        double distance = agent.distanceTo(targetPlayer);
        if (distance > 3.0) {
            agent.getNavigation().moveTo(targetPlayer, 1.0);
        } else if (distance < 2.0) {
            agent.getNavigation().stop();
        }
    }

    @Override
    protected void onCancel() {
        agent.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Follow player " + playerName;
    }

    private void findPlayer() {
        java.util.List<? extends Player> players = agent.level().players();
        
        // First try exact name match
        for (Player player : players) {
            if (player.getName().getString().equalsIgnoreCase(playerName)) {
                targetPlayer = player;
                return;
            }
        }
        
        if (playerName != null && (playerName.contains("PLAYER") || playerName.contains("NAME") || 
            playerName.equalsIgnoreCase("me") || playerName.equalsIgnoreCase("you") || playerName.isEmpty())) {
            Player nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            
            for (Player player : players) {
                double distance = agent.distanceTo(player);
                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
            
            if (nearest != null) {
                targetPlayer = nearest;
                playerName = nearest.getName().getString(); // Update to actual name
                com.xenchinryu7.mcagenticlabs.AgentsMod.LOGGER.info("Agent '{}' following nearest player: {}", 
                    agent.getAgentName(), playerName);
            }
        }
    }
}

