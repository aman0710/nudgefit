package com.nudgefit.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Builds dynamic prompts by loading templates from src/main/resources/prompts/
 * and injecting user-specific variables using {variable} placeholders.
 */
@Component
@Slf4j
public class PromptBuilder {

    /**
     * Loads a prompt template from the classpath and replaces {variable} placeholders
     * with the provided values.
     *
     * @param templateName The template filename (e.g., "intent-classification.txt")
     * @param variables    Map of placeholder names to their values
     * @return The populated prompt string
     */
    public String build(String templateName, Map<String, String> variables) {
        String template = loadTemplate(templateName);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            template = template.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return template;
    }

    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("prompts/" + templateName);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load prompt template: {}", templateName, e);
            throw new RuntimeException("Could not load prompt template: " + templateName, e);
        }
    }
}
