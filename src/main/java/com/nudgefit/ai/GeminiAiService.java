package com.nudgefit.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nudgefit.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the Gemini REST API and parses JSON responses into target DTOs.
 * Includes retry logic (max 2 retries with 1s delay).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private final WebClient geminiWebClient;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    /**
     * Sends a prompt to Gemini and parses the JSON response into the target class.
     *
     * @param prompt      The prompt text to send
     * @param targetClass The class to deserialize the response into
     * @return Parsed response object, or null if the API call fails
     */
    public <T> T call(String prompt, Class<T> targetClass) {
        try {
            String endpoint = String.format(
                    "/models/%s:generateContent?key=%s",
                    geminiConfig.getModel(),
                    geminiConfig.getApiKey()
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0.7
                    )
            );

            String responseJson = geminiWebClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                    .block();

            // Extract the text from candidates[0].content.parts[0].text
            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            return objectMapper.readValue(text, targetClass);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Sends a prompt to Gemini and returns the raw text response (no JSON parsing).
     * Used for coaching responses that are plain text.
     */
    public String callForText(String prompt) {
        try {
            String endpoint = String.format(
                    "/models/%s:generateContent?key=%s",
                    geminiConfig.getModel(),
                    geminiConfig.getApiKey()
            );

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.7
                    )
            );

            String responseJson = geminiWebClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                    .block();

            Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            return null;
        }
    }
}
