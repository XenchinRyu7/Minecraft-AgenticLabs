package com.xenchinryu7.mcagenticlabs.action.actions;

import com.xenchinryu7.mcagenticlabs.action.ActionResult;
import com.xenchinryu7.mcagenticlabs.action.Task;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;

public class GatherResourceAction extends BaseAction {
    private String resourceType;
    private int quantity;

    public GatherResourceAction(AgentEntity agent, Task task) {
        super(agent, task);
    }

    @Override
    protected void onStart() {
        resourceType = task.getStringParameter("resource");
        quantity = task.getIntParameter("quantity", 1);
        
        // This is essentially a smart wrapper around mining that:
        // - Mines them
        
        result = ActionResult.failure("Resource gathering not yet fully implemented", false);
    }

    @Override
    protected void onTick() {
    }

    @Override
    protected void onCancel() {
        agent.getNavigation().stop();
    }

    @Override
    public String getDescription() {
        return "Gather " + quantity + " " + resourceType;
    }
}

