package com.xenchinryu7.mcagenticlabs.memory;

import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class AgentMemory {
    private final AgentEntity agent;
    private String currentGoal;
    private final Queue<String> taskQueue;
    private final LinkedList<String> recentActions;
    private static final int MAX_RECENT_ACTIONS = 20;

    public AgentMemory(AgentEntity agent) {
        this.agent = agent;
        this.currentGoal = "";
        this.taskQueue = new LinkedList<>();
        this.recentActions = new LinkedList<>();
    }

    public String getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(String goal) {
        this.currentGoal = goal;
    }

    public void addAction(String action) {
        recentActions.addLast(action);
        if (recentActions.size() > MAX_RECENT_ACTIONS) {
            recentActions.removeFirst();
        }
    }

    public List<String> getRecentActions(int count) {
        int size = Math.min(count, recentActions.size());
        List<String> result = new ArrayList<>();
        
        int startIndex = Math.max(0, recentActions.size() - count);
        for (int i = startIndex; i < recentActions.size(); i++) {
            result.add(recentActions.get(i));
        }
        
        return result;
    }

    public void clearTaskQueue() {
        taskQueue.clear();
        currentGoal = "";
    }

    public void saveToNBT(CompoundTag tag) {
        tag.putString("CurrentGoal", currentGoal);
        
        ListTag actionsList = new ListTag();
        for (String action : recentActions) {
            actionsList.add(StringTag.valueOf(action));
        }
        tag.put("RecentActions", actionsList);
    }

    public void loadFromNBT(CompoundTag tag) {
        if (tag.contains("CurrentGoal")) {
            Optional<String> goalOpt = tag.getString("CurrentGoal");
            if (goalOpt.isPresent()) {
                currentGoal = goalOpt.get();
            } else {
                currentGoal = "";
            }
        }
        
        if (tag.contains("RecentActions")) {
            recentActions.clear();
            Optional<ListTag> actionsOpt = tag.getList("RecentActions");
            if (actionsOpt.isPresent()) {
                ListTag actionsList = actionsOpt.get();
                for (int i = 0; i < actionsList.size(); i++) {
                    Optional<String> strOpt = actionsList.getString(i);
                    if (strOpt.isPresent()) {
                        recentActions.add(strOpt.get());
                    }
                }
            }
        }
    }
}

