package com.nudgefit.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nudgefit.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Calls the Gemini REST API and parses JSON responses into target DTOs.
 * 
 * Rate Limiting Strategy:
 * - enforceRateLimit() proactively spaces out requests (1 every 4s) to stay within the free tier.
 * - If a 429 is received despite this (e.g., daily quota exhausted), we fail fast and return null.
 * - Only transient 5xx server errors are retried (once, after 2s).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiAiService {

    private final WebClient geminiWebClient;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    private static long lastCallTime = 0;

    /**
     * Proactively spaces out API calls to stay within the free tier rate limit
     * of 15 Requests Per Minute (1 request every 4 seconds).
     */
    private synchronized void enforceRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastCallTime;
        long requiredDelay = 4000; // 4000ms = 1 request per 4 seconds

        if (elapsed < requiredDelay) {
            try {
                Thread.sleep(requiredDelay - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();
    }

    /**
     * Sends a prompt to Gemini and parses the JSON response into the target class.
     *
     * @param prompt      The prompt text to send
     * @param targetClass The class to deserialize the response into
     * @return Parsed response object, or null if the API call fails
     */
    public <T> T call(String prompt, Class<T> targetClass) {
        enforceRateLimit();
        try {
            String responseJson = executeGeminiCall(prompt, true);
            if (responseJson == null) return null;

            String text = extractTextFromResponse(responseJson);
            return objectMapper.readValue(text, targetClass);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sends a prompt to Gemini and returns the raw text response (no JSON parsing).
     * Used for coaching responses that are plain text.
     */
    public String callForText(String prompt) {
        enforceRateLimit();
        try {
            String responseJson = executeGeminiCall(prompt, false);
            if (responseJson == null) return null;

            return extractTextFromResponse(responseJson);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Core method that executes the Gemini API call.
     * - Retries once (after 5s) for 429 rate limits (handles per-minute quota).
     * - Retries once (after 5s) for transient 5xx server errors.
     * - If retry also fails (daily quota exhausted), returns null immediately.
     */
    private String executeGeminiCall(String prompt, boolean jsonMode) {
        String endpoint = String.format(
                "/models/%s:generateContent?key=%s",
                geminiConfig.getModel(),
                geminiConfig.getApiKey()
        );

        Map<String, Object> generationConfig = jsonMode
                ? Map.of("responseMimeType", "application/json", "temperature", 0.7)
                : Map.of("temperature", 0.7);

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                ),
                "generationConfig", generationConfig
        );

        try {
            return geminiWebClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(5))
                            .filter(t -> {
                                if (t instanceof WebClientResponseException ex) {
                                    if (ex.getStatusCode().value() == 429) {
                                        log.warn("Gemini API rate limited (429), will retry once in 5s...");
                                        return true;
                                    }
                                    if (ex.getStatusCode().is5xxServerError()) {
                                        log.warn("Gemini API server error ({}), will retry once in 5s...", ex.getStatusCode());
                                        return true;
                                    }
                                }
                                return false;
                            }))
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("Gemini API failed after retry: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            log.error("Gemini API request failed: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Extracts the text content from the Gemini API response JSON.
     * Path: candidates[0].content.parts[0].text
     */
    private String extractTextFromResponse(String responseJson) throws Exception {
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        return (String) parts.get(0).get("text");
    }
}
