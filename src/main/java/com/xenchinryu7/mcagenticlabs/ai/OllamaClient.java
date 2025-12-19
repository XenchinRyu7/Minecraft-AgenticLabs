package com.xenchinryu7.mcagenticlabs.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xenchinryu7.mcagenticlabs.AgentsMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Client for local Ollama API
 * Runs AI models locally without internet or API keys
 */
public class OllamaClient {
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    
    private final HttpClient client;
    private final String model;

    public OllamaClient() {
        this.model = "mistral:7b"; // Default model, can be made configurable later
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("prompt", systemPrompt + "\n" + userPrompt);
            requestBody.addProperty("stream", false);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                if (jsonResponse.has("response")) {
                    return jsonResponse.get("response").getAsString();
                }
                AgentsMod.LOGGER.error("Unexpected Ollama response format: {}", response.body());
                return null;
            } else {
                AgentsMod.LOGGER.error("Ollama API error: {} - {}", response.statusCode(), response.body());
                return null;
            }
            
        } catch (Exception e) {
            AgentsMod.LOGGER.error("Error calling Ollama API", e);
            return null;
        }
    }
}