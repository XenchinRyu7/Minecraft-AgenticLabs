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
 * Client for Google Gemini API
 * FREE tier: 15 RPM, 1500 RPD
 * Paid: ~10x cheaper than GPT-3.5
 * Using gemini-2.5-flash with high token limit for thinking mode
 */
public class GeminiClient {
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    private final HttpClient client;
    private final String apiKey;

    public GeminiClient() {
        this.apiKey = AgentConfig.OPENAI_API_KEY.get(); // We'll use the same config for now
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            AgentsMod.LOGGER.error("Gemini API key not configured!");
            return null;
        }

        JsonObject requestBody = buildRequestBody(systemPrompt, userPrompt);
        String urlWithKey = GEMINI_API_URL + "?key=" + apiKey;
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                AgentsMod.LOGGER.error("Gemini API request failed: {}", response.statusCode());
                AgentsMod.LOGGER.error("Response body: {}", response.body());
                return null;
            }

            String responseBody = response.body();
            if (responseBody == null || responseBody.isEmpty()) {
                AgentsMod.LOGGER.error("Gemini API returned empty response");
                return null;
            }

            return parseResponse(responseBody);
            
        } catch (Exception e) {
            AgentsMod.LOGGER.error("Error communicating with Gemini API", e);
            return null;
        }
    }

    private JsonObject buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject body = new JsonObject();
        
        // Gemini uses "contents" array with "parts"
        JsonArray contents = new JsonArray();
        
        // System instruction (Gemini 1.5+ format)
        JsonObject systemContent = new JsonObject();
        systemContent.addProperty("role", "user");
        JsonArray systemParts = new JsonArray();
        JsonObject systemPart = new JsonObject();
        systemPart.addProperty("text", systemPrompt + "\n\n" + userPrompt);
        systemParts.add(systemPart);
        systemContent.add("parts", systemParts);
        contents.add(systemContent);
        
        body.add("contents", contents);
        
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", AgentConfig.TEMPERATURE.get());
        generationConfig.addProperty("maxOutputTokens", AgentConfig.MAX_TOKENS.get());
        body.add("generationConfig", generationConfig);
        
        return body;
    }

    private String parseResponse(String responseBody) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            
            // Gemini response format: candidates[0].content.parts[0].text
            if (json.has("candidates") && json.getAsJsonArray("candidates").size() > 0) {
                JsonObject firstCandidate = json.getAsJsonArray("candidates").get(0).getAsJsonObject();
                
                if (firstCandidate.has("finishReason")) {
                    String finishReason = firstCandidate.get("finishReason").getAsString();
                    if ("MAX_TOKENS".equals(finishReason)) {
                        AgentsMod.LOGGER.error("Gemini response was cut off due to MAX_TOKENS limit");
                    }
                }
                
                if (firstCandidate.has("content")) {
                    JsonObject content = firstCandidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject firstPart = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (firstPart.has("text")) {
                            return firstPart.get("text").getAsString();
                        }
                    } else {
                        AgentsMod.LOGGER.error("Gemini response has no 'parts' in content - response may have been cut off");
                    }
                }
            }
            
            AgentsMod.LOGGER.error("Unexpected Gemini response format: {}", responseBody);
            return null;
            
        } catch (Exception e) {
            AgentsMod.LOGGER.error("Error parsing Gemini response", e);
            return null;
        }
    }
}

