package com.nudgefit.service;

import com.nudgefit.ai.GeminiAiService;
import com.nudgefit.ai.PromptBuilder;
import com.nudgefit.model.dto.IntentClassificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntentClassifierService {

    private final GeminiAiService geminiAiService;
    private final PromptBuilder promptBuilder;
    private final ConversationContextService contextService;

    public IntentClassificationResponse classifyIntent(String phoneNumber, String userMessage) {
        // Fetch the last 5 messages for context
        List<String> recentMessages = contextService.getRecentMessages(phoneNumber, 5);
        String conversationHistory = String.join("\n", recentMessages);

        String prompt = promptBuilder.buildPrompt("intent-classification.txt", Map.of(
                "user_message", userMessage,
                "conversation_history", conversationHistory
        ));

        try {
            IntentClassificationResponse response = geminiAiService.call(prompt, IntentClassificationResponse.class);
            if (response != null) {
                return response;
            }
        } catch (Exception e) {
            log.error("Failed to classify intent for {}: {}", phoneNumber, e.getMessage());
        }

        // Fallback
        return new IntentClassificationResponse(com.nudgefit.model.enums.Intent.GENERAL_CHAT, 0.0, null);
    }
}
