package com.capstone.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.capstone.config.ConfigManager;

public class GroqApiClient {

    private static final Logger log = LoggerFactory.getLogger(GroqApiClient.class);
    
    // Fetch the new key from your .env file
    private static final String API_KEY = ConfigManager.getInstance().getString("ai.api.key"); 
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    public static String askAi(String prompt) {
        if (API_KEY == null || API_KEY.isEmpty()) {
            log.error("AI API Key is missing!");
            return "AI Analysis unavailable.";
        }

        // Clean the prompt
        String safePrompt = prompt.replace("\"", "\\\"").replace("\n", " ");

        // Build the JSON payload for Groq (using Meta's Llama 3 model)
        String requestBody = "{\n" +
                "  \"model\": \"llama-3.3-70b-versatile\",\n" +
                "  \"messages\": [\n" +
                "    {\n" +
                "      \"role\": \"user\",\n" +
                "      \"content\": \"" + safePrompt + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        log.info("Sending defect to Groq AI for analysis...");

        // Make the REST call using RestAssured
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + API_KEY)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .post(GROQ_URL);

        if (response.statusCode() == 200) {
            String aiResponse = response.jsonPath().getString("choices[0].message.content");
            log.info("AI Response received successfully.");
            return aiResponse;
        } else {
            log.error("AI API Error: Status Code " + response.statusCode());
            log.error("Error Details: " + response.getBody().asString());
            return "Failed to get response from AI.";
        }
    }
}