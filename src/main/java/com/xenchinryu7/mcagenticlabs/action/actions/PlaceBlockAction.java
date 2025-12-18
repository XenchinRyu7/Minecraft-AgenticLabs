package com.xenchinryu7.mcagenticlabs.action.actions;

import com.xenchinryu7.mcagenticlabs.action.ActionResult;
import com.xenchinryu7.mcagenticlabs.action.Task;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class PlaceBlockAction extends BaseAction {
    private Block blockToPlace;
    private BlockPos targetPos;
    private int ticksRunning;
    private static final int MAX_TICKS = 200;

    public PlaceBlockAction(AgentEntity agent, Task task) {
        super(agent, task);
    }

    @Override
    protected void onStart() {
        String blockName = task.getStringParameter("block");
        int x = task.getIntParameter("x", 0);
        int y = task.getIntParameter("y", 0);
        int z = task.getIntParameter("z", 0);
        
        targetPos = new BlockPos(x, y, z);
        ticksRunning = 0;
        
        blockToPlace = parseBlock(blockName, agent.level());
        
        if (blockToPlace == null || blockToPlace == Blocks.AIR) {
            result = ActionResult.failure("Invalid block type: " + blockName);
            return;
        }
        
    }

    @Override
    protected void onTick() {
        ticksRunning++;
        
        if (ticksRunning > MAX_TICKS) {
            result = ActionResult.failure("Place block timeout");
            return;
        }
        
        if (!agent.blockPosition().closerThan(targetPos, 5.0)) {
            agent.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.0);
            return;
        }
        
        BlockState currentState = agent.level().getBlockState(targetPos);
        if (!currentState.isAir() && !currentState.liquid()) {
            result = ActionResult.failure("Position is not empty");
            return;
        }
        
        agent.level().setBlock(targetPos, blockToPlace.defaultBlockState(), 3);
        result = ActionResult.success("Placed " + blockToPlace.getName().getString());
    }

    @Override
    protected void onCancel() {
        agent.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Place " + blockToPlace.getName().getString() + " at " + targetPos;
    }

    private Block parseBlock(String blockName, Level level) {
        blockName = blockName.toLowerCase().replace(" ", "_");
        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }
        ResourceLocation identifier = ResourceLocation.parse(blockName);
        Optional<Block> blockOpt = level.registryAccess().lookup(Registries.BLOCK).orElseThrow().getOptional(ResourceKey.create(Registries.BLOCK, identifier));
        return blockOpt.orElse(null);
    }
}

