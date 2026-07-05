package com.sqlagent.sqlqueryagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlagent.sqlqueryagent.model.QueryAnalysis;
import com.sqlagent.sqlqueryagent.model.RiskFlag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class QueryAnalysisAgent {

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private PromptBuilder promptBuilder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public QueryAnalysis analyze(String schemaDescription, String userRequest) {

        String prompt = promptBuilder.buildPrompt(schemaDescription, userRequest);
        String rawResponse = geminiClient.generateContent(prompt);
        String cleanedResponse = cleanJson(rawResponse);

        GeminiResult result;
        try {
            result = objectMapper.readValue(cleanedResponse, GeminiResult.class);
        } catch (Exception firstFailure) {
            String retryPrompt = prompt
                    + "\n\nYour previous response was not valid JSON. "
                    + "Return ONLY the JSON object described above, with no extra text, no explanation, and no markdown code fences.";

            String retryRaw = geminiClient.generateContent(retryPrompt);
            String retryCleaned = cleanJson(retryRaw);

            try {
                result = objectMapper.readValue(retryCleaned, GeminiResult.class);
            } catch (Exception secondFailure) {
                throw new RuntimeException(
                        "Gemini did not return valid JSON even after a retry. Raw response: " + retryCleaned,
                        secondFailure
                );
            }
        }

        QueryAnalysis analysis = new QueryAnalysis();
        analysis.setSchemaDescription(schemaDescription);
        analysis.setNaturalLanguageRequest(userRequest);
        analysis.setGeneratedSql(result.generatedSql);
        analysis.setRiskLevel(result.riskLevel);
        analysis.setRiskFlags(result.riskFlags);
        analysis.setCreatedAt(Instant.now());

        return analysis;
    }

    private String cleanJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(json)?", "");
            trimmed = trimmed.replaceFirst("```$", "");
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    static class GeminiResult {
        public String generatedSql;
        public String riskLevel;
        public List<RiskFlag> riskFlags;
    }
}