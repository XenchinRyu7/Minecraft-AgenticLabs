package com.xenchinryu7.mcagenticlabs.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xenchinryu7.mcagenticlabs.AgentsMod;
import com.xenchinryu7.mcagenticlabs.config.AgentConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for Groq API - BLAZING FAST inference
 * FREE tier: 30 RPM, 14,400 RPD
 * Speed: 0.5-2 seconds (vs Gemini's 10-30s)
 */
public class GroqClient {
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    
    private final HttpClient client;
    private final String apiKey;

    public GroqClient() {
        this.apiKey = AgentConfig.OPENAI_API_KEY.get(); // Reuse same config field
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            AgentsMod.LOGGER.error("Groq API key is not set in the config.");
            return null;
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "llama-3.1-8b-instant");
        
        JsonArray messages = new JsonArray();
        
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);
        requestBody.addProperty("max_tokens", 500); // Keep it short for speed
        requestBody.addProperty("temperature", 0.7);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GROQ_API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                return jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            } else {
                AgentsMod.LOGGER.error("Groq API request failed: {} ", response.statusCode());
                AgentsMod.LOGGER.error("Response body: {}", response.body());
                return null;
            }
        } catch (Exception e) {
            AgentsMod.LOGGER.error("Error sending request to Groq API", e);
            return null;
        }
    }
}

