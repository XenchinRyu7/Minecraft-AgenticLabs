package com.xenchinryu7.mcagenticlabs.action.actions;

import com.xenchinryu7.mcagenticlabs.action.ActionResult;
import com.xenchinryu7.mcagenticlabs.action.Task;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class CombatAction extends BaseAction {
    private String targetType;
    private LivingEntity target;
    private int ticksRunning;
    private int ticksStuck;
    private double lastX, lastZ;
    private static final int MAX_TICKS = 600;
    private static final double ATTACK_RANGE = 3.5;

    public CombatAction(AgentEntity agent, Task task) {
        super(agent, task);
    }

    @Override
    protected void onStart() {
        targetType = task.getStringParameter("target");
        ticksRunning = 0;
        ticksStuck = 0;
        
        // Make sure we're not flying (in case we were building)
        agent.setFlying(false);
        
        agent.setInvulnerableBuilding(true);
        
        findTarget();
        
        if (target == null) {
            com.xenchinryu7.mcagenticlabs.agentMod.LOGGER.warn("Agent '{}' no targets nearby", agent.getAgentName());
        }
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            // Combat complete - clean up and disable invulnerability
            agent.setInvulnerableBuilding(false);
            agent.setSprinting(false);
            agent.getNavigation().stop();
            com.xenchinryu7.mcagenticlabs.agentMod.LOGGER.info("Agent '{}' combat complete, invulnerability disabled", 
                agent.getAgentName());
            result = ActionResult.success("Combat complete");
            return;
        }
        
        // Re-search for targets periodically or if current target is invalid
        if (target == null || !target.isAlive() || target.isRemoved()) {
            if (ticksRunning % 20 == 0) {
                findTarget();
            }
            if (target == null) {
                return; // Keep searching
            }
        }
        
        double distance = agent.distanceTo(target);
        
        agent.setSprinting(true);
        agent.getNavigation().moveTo(target, 2.5); // High speed multiplier for sprinting
        
        double currentX = agent.getX();
        double currentZ = agent.getZ();
        if (Math.abs(currentX - lastX) < 0.1 && Math.abs(currentZ - lastZ) < 0.1) {
            ticksStuck++;
            
            if (ticksStuck > 40 && distance > ATTACK_RANGE) {
                // Teleport 4 blocks closer to target
                double dx = target.getX() - agent.getX();
                double dz = target.getZ() - agent.getZ();
                double dist = Math.sqrt(dx*dx + dz*dz);
                double moveAmount = Math.min(4.0, dist - ATTACK_RANGE);
                
                agent.teleportTo(
                    agent.getX() + (dx/dist) * moveAmount,
                    agent.getY(),
                    agent.getZ() + (dz/dist) * moveAmount
                );
                ticksStuck = 0;
                com.xenchinryu7.mcagenticlabs.agentMod.LOGGER.info("Agent '{}' was stuck, teleported closer to target", 
                    agent.getAgentName());
            }
        } else {
            ticksStuck = 0;
        }
        lastX = currentX;
        lastZ = currentZ;
        
        if (distance <= ATTACK_RANGE) {
            agent.doHurtTarget(target);
            agent.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            
            // Attack 3 times per second (every 6-7 ticks)
            if (ticksRunning % 7 == 0) {
                agent.doHurtTarget(target);
            }
        }
    }

    @Override
    protected void onCancel() {
        agent.setInvulnerableBuilding(false);
        agent.getNavigation().stop();
        agent.setSprinting(false);
        agent.setFlying(false);
        target = null;
        com.xenchinryu7.mcagenticlabs.agentMod.LOGGER.info("Agent '{}' combat cancelled, invulnerability disabled", 
            agent.getAgentName());
    }

    @Override
    public String getDescription() {
        return "Attack " + targetType;
    }

    private void findTarget() {
        AABB searchBox = agent.getBoundingBox().inflate(32.0);
        List<Entity> entities = agent.level().getEntities(agent, searchBox);
        
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity living && isValidTarget(living)) {
                double distance = agent.distanceTo(living);
                if (distance < nearestDistance) {
                    nearest = living;
                    nearestDistance = distance;
                }
            }
        }
        
        target = nearest;
        if (target != null) {
            com.xenchinryu7.mcagenticlabs.agentMod.LOGGER.info("Agent '{}' locked onto: {} at {}m", 
                agent.getAgentName(), target.getType().toString(), (int)nearestDistance);
        }
    }

    private boolean isValidTarget(LivingEntity entity) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }
        
        // Don't attack other agents or players
        if (entity instanceof agentEntity || entity instanceof net.minecraft.world.entity.player.Player) {
            return false;
        }
        
        String targetLower = targetType.toLowerCase();
        
        // Match ANY hostile mob
        if (targetLower.contains("mob") || targetLower.contains("hostile") || 
            targetLower.contains("monster") || targetLower.equals("any")) {
            return entity instanceof Monster;
        }
        
        // Match specific entity type
        String entityTypeName = entity.getType().toString().toLowerCase();
        return entityTypeName.contains(targetLower);
    }
}
