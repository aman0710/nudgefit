package com.nudgefit.controller;

import com.nudgefit.model.dto.MessageReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Twilio sends POST with application/x-www-form-urlencoded.
     * We extract Body, From, and MessageSid, then return an empty TwiML response immediately.
     * All processing happens asynchronously via MessageProcessorService.
     */
    @PostMapping(value = "/whatsapp",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public String handleIncomingMessage(
            @RequestParam("Body") String body,
            @RequestParam("From") String from,
            @RequestParam("MessageSid") String messageSid) {

        // Strip "whatsapp:" prefix from the phone number
        String phoneNumber = from.replace("whatsapp:", "");

        log.info("Received WhatsApp message from {} (sid: {}): {}", phoneNumber, messageSid, body);

        // Publish event to be processed asynchronously
        eventPublisher.publishEvent(new MessageReceivedEvent(phoneNumber, body));

        // Return empty TwiML response to Twilio (we'll reply via the API separately)
        return "<Response></Response>";
    }
}
