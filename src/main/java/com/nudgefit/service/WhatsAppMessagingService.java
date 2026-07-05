package com.nudgefit.service;

import com.nudgefit.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Sends WhatsApp messages via the Twilio SDK.
 * Includes simple retry logic (max 2 attempts) as per blueprint section 15.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppMessagingService {

    private final TwilioConfig twilioConfig;

    private static final int MAX_RETRIES = 2;

    /**
     * Sends a WhatsApp message to the given phone number.
     *
     * @param toPhoneNumber The recipient phone number (e.g., "+919876543210")
     * @param messageBody   The message text to send
     */
    public void sendMessage(String toPhoneNumber, String messageBody) {
        String to = "whatsapp:" + toPhoneNumber;
        String from = twilioConfig.getWhatsappNumber();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Message message = Message.creator(
                        new PhoneNumber(to),
                        new PhoneNumber(from),
                        messageBody
                ).create();

                log.info("Message sent to {} (sid: {})", toPhoneNumber, message.getSid());
                return;

            } catch (Exception e) {
                log.error("Twilio send attempt {}/{} failed for {}: {}",
                        attempt, MAX_RETRIES, toPhoneNumber, e.getMessage());

                if (attempt == MAX_RETRIES) {
                    log.error("All retry attempts exhausted for {}", toPhoneNumber);
                }
            }
        }
    }
}
