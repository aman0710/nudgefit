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
 * - enforceRateLimit() proactively spaces out requests (1 every 4s) to stay
 * within the free tier.
 * - If a 429 is received despite this (e.g., daily quota exhausted), we fail
 * fast and return null.
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
        long requiredDelay = 4500; // 4500ms = safely under 15 requests per minute

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
        int maxAttempts = 2;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            enforceRateLimit();
            try {
                String responseJson = executeGeminiCall(prompt, true);
                if (responseJson == null || responseJson.isBlank()) {
                    log.warn("Gemini API returned null or empty response; returning null for {}",
                            targetClass.getSimpleName());
                    return null;
                }
                // Extract text and ensure we have a complete JSON object
                String text = extractTextFromResponse(responseJson);
                log.debug("Gemini raw text for {}: {}", targetClass.getSimpleName(), text);
                String json = text.trim();
                // Always extract from first '{' to last '}' to strip any stray characters
                int startIdx = json.indexOf('{');
                int endIdx = json.lastIndexOf('}');
                if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                    json = json.substring(startIdx, endIdx + 1);
                } else {
                    log.warn("Unable to locate JSON object in Gemini response; returning null");
                    return null;
                }
                try {
                    return objectMapper.readValue(json, targetClass);
                } catch (Exception jsonEx) {
                    // Malformed or truncated JSON – retry if we have attempts left
                    log.warn("Gemini returned invalid JSON (attempt {}/{}): {}", attempt, maxAttempts,
                            jsonEx.getMessage());
                    if (attempt < maxAttempts) {
                        log.info("Retrying Gemini call for {}...", targetClass.getSimpleName());
                        continue;
                    }
                    log.error("Gemini returned invalid JSON after {} attempts; returning null", maxAttempts);
                    return null;
                }
            } catch (Exception e) {
                log.error("Gemini API call failed: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Sends a prompt to Gemini and returns the raw text response (no JSON parsing).
     * Used for coaching responses that are plain text.
     */
    public String callForText(String prompt) {
        enforceRateLimit();
        try {
            String responseJson = executeGeminiCall(prompt, false);
            if (responseJson == null)
                throw new RuntimeException("executeGeminiCall returned null (likely rate limit or 500)");

            return extractTextFromResponse(responseJson);

        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("Gemini API call failed: " + e.getMessage(), e);
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
                geminiConfig.getApiKey());

        // Disable thinking to prevent thinking tokens from consuming output budget
        Map<String, Object> generationConfig = new java.util.HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 8192);
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", prompt)))));
        requestBody.put("generationConfig", generationConfig);

        String maskedKey = geminiConfig.getApiKey() != null && geminiConfig.getApiKey().length() > 4
                ? "..." + geminiConfig.getApiKey().substring(geminiConfig.getApiKey().length() - 4)
                : "unknown";
        log.debug("Executing Gemini POST request to {} with API Key ending in {}", geminiConfig.getModel(), maskedKey);

        try {
            String result = geminiWebClient.post()
                    .uri(endpoint)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(5))
                            .filter(t -> {
                                if (t instanceof WebClientResponseException ex) {
                                    if (ex.getStatusCode().value() == 429 || ex.getStatusCode().is5xxServerError()) {
                                        log.warn("Gemini API error ({}), waiting 5s to let quota reset...",
                                                ex.getStatusCode());
                                        synchronized (this) {
                                            lastCallTime = System.currentTimeMillis() + 5000;
                                        }
                                        return true;
                                    }
                                }
                                return false;
                            }))
                    .block();
            log.debug("Gemini raw API response (first 500 chars): {}",
                    result != null ? result.substring(0, Math.min(result.length(), 500)) : "null");
            return result;
        } catch (WebClientResponseException ex) {
            log.error("Gemini API HTTP error: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return null;
        } catch (Exception ex) {
            log.error("Gemini API request failed: {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Extracts the text content from the Gemini API response JSON.
     * Path: candidates[0].content.parts[0].text
     */
    private String extractTextFromResponse(String responseJson) throws Exception {
        // Parse the whole response safely
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
        // Navigate to candidates list safely
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("No candidates found in Gemini response");
        }
        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        if (content == null) {
            throw new IllegalStateException("No content found in Gemini candidate");
        }
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
        if (parts == null || parts.isEmpty()) {
            throw new IllegalStateException("No parts found in Gemini content");
        }
        // Find the first part that contains a 'text' field
        for (Map<String, Object> part : parts) {
            if (part.containsKey("text")) {
                Object txt = part.get("text");
                return txt != null ? txt.toString() : "";
            }
        }
        // If no text field present, return empty string
        return "";
    }

}
