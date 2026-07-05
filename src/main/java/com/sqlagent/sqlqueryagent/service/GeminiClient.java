package com.sqlagent.sqlqueryagent.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiClient {

    // Unga original validation API key-ah inject panniyaachu
    private final String apiKey = "AQ.Ab8RN6Kzx0y8CvnpyUMZSnfp8-agpOCatCNGHRGrggXVIko3Fg";

    // Latest Gemini 2.5 Model String Mapping updated!
    private final String model = "gemini-2.5-flash";

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    @SuppressWarnings("unchecked")
    public String generateContent(String prompt) {
        System.out.println("=================================");
        System.out.println("Running Gemini API Pipeline Connect...");
        System.out.println("Target Model   : " + model);
        System.out.println("=================================");

        // Standard Gemini JSON payload structure
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", prompt)
                                )
                        )
                )
        );

        try {
            Map<String, Object> response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/" + model + ":generateContent")
                            .queryParam("key", apiKey)
                            .build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractTextFromResponse(response);

        } catch (WebClientResponseException e) {
            System.out.println("========== GEMINI API REJECTION ERROR ==========");
            System.out.println("Status Code : " + e.getStatusCode());
            System.out.println("Response Body : " + e.getResponseBodyAsString());
            throw new RuntimeException("Gemini Remote Error: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("========== UNEXPECTED RUNTIME ERROR ==========");
            e.printStackTrace();
            throw new RuntimeException("Internal Crash: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            if (response == null || !response.containsKey("candidates")) {
                return "Error: Empty response structure.";
            }
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "Error parsing JSON mapping: " + e.getMessage();
        }
    }
}