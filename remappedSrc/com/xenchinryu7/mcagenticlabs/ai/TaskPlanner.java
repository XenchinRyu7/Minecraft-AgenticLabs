package com.xenchinryu7.mcagenticlabs.ai;

import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.action.Task;
import com.xenchinryu7.mcagenticlabs.config.AgentConfig;
import com.xenchinryu7.mcagenticlabs.entity.AgentEntity;
import com.xenchinryu7.mcagenticlabs.memory.WorldKnowledge;

import java.util.List;

public class TaskPlanner {
    private final OpenAIClient openAIClient;
    private final GeminiClient geminiClient;
    private final GroqClient groqClient;
    private final OllamaClient ollamaClient;
    
    private static String currentProvider = "ollama"; // Default to ollama

    public TaskPlanner() {
        this.openAIClient = new OpenAIClient();
        this.geminiClient = new GeminiClient();
        this.groqClient = new GroqClient();
        this.ollamaClient = new OllamaClient();
    }

    public ResponseParser.ParsedResponse planTasks(AgentEntity agent, String command) {
        try {
            String systemPrompt = PromptBuilder.buildSystemPrompt();
            WorldKnowledge worldKnowledge = new WorldKnowledge(agent);
            String userPrompt = PromptBuilder.buildUserPrompt(agent, command, worldKnowledge);
            
            String provider = "ollama";
            AgentsMod.LOGGER.info("Requesting AI plan for Agent '{}' using {}: {}", agent.getAgentName(), provider, command);
            
            String response = getAIResponse(provider, systemPrompt, userPrompt);
            
            if (response == null) {
                AgentsMod.LOGGER.error("Failed to get AI response for command: {}", command);
                return null;
            }            ResponseParser.ParsedResponse parsedResponse = ResponseParser.parseAIResponse(response);
            
            if (parsedResponse == null) {
                AgentsMod.LOGGER.error("Failed to parse AI response");
                return null;
            }
            
            AgentsMod.LOGGER.info("Plan: {} ({} tasks)", parsedResponse.getPlan(), parsedResponse.getTasks().size());
            
            return parsedResponse;
            
        } catch (Exception e) {
            AgentsMod.LOGGER.error("Error planning tasks", e);
            return null;
        }
    }

    private String getAIResponse(String provider, String systemPrompt, String userPrompt) {
        String response = switch (provider) {
            case "groq" -> groqClient.sendRequest(systemPrompt, userPrompt);
            case "gemini" -> geminiClient.sendRequest(systemPrompt, userPrompt);
            case "openai" -> openAIClient.sendRequest(systemPrompt, userPrompt);
            case "ollama" -> ollamaClient.sendRequest(systemPrompt, userPrompt);
            default -> {
                AgentsMod.LOGGER.warn("Unknown AI provider '{}', using Ollama", provider);
                yield ollamaClient.sendRequest(systemPrompt, userPrompt);
            }
        };
        
        if (response == null && !provider.equals("ollama")) {
            AgentsMod.LOGGER.warn("{} failed, trying Ollama as fallback", provider);
            response = ollamaClient.sendRequest(systemPrompt, userPrompt);
        }
        
        return response;
    }

    public boolean validateTask(Task task) {
        String action = task.getAction();
        
        return switch (action) {
            case "pathfind" -> task.hasParameters("x", "y", "z");
            case "mine" -> task.hasParameters("block", "quantity");
            case "place" -> task.hasParameters("block", "x", "y", "z");
            case "craft" -> task.hasParameters("item", "quantity");
            case "attack" -> task.hasParameters("target");
            case "follow" -> task.hasParameters("player");
            case "gather" -> task.hasParameters("resource", "quantity");
            case "build" -> task.hasParameters("structure", "blocks", "dimensions");
            default -> {
                AgentsMod.LOGGER.warn("Unknown action type: {}", action);
                yield false;
            }
        };
    }

    public static void setProvider(String provider) {
        currentProvider = provider.toLowerCase();
        AgentsMod.LOGGER.info("AI provider set to: {}", currentProvider);
    }

    public static String getCurrentProvider() {
        return currentProvider;
    }

