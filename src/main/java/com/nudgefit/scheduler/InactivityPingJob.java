package com.nudgefit.scheduler;

import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.repository.UserRepository;
import com.nudgefit.service.WhatsAppMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityPingJob {

    private final UserRepository userRepository;
    private final WhatsAppMessagingService messagingService;

    // Runs every day at 12:00 PM (Noon)
    @Scheduled(cron = "0 0 12 * * *", zone = "Asia/Kolkata")
    public void sendInactivityPing() {
        log.info("Starting Inactivity Ping job...");
        LocalDateTime twoDaysAgo = LocalDateTime.now().minusDays(2);
        
        List<User> inactiveUsers = userRepository.findAll().stream()
                .filter(u -> u.getConversationState() == ConversationState.ACTIVE)
                .filter(u -> u.getLastMessageAt() != null && u.getLastMessageAt().isBefore(twoDaysAgo))
                .toList();

        for (User user : inactiveUsers) {
            String message = String.format(
                    "Hey %s! 👋 I haven't heard from you in a couple of days. " +
                    "Just checking in—how are things going with your goals? " +
                    "Let me know if you need any adjustments or just some motivation! 🚀",
                    user.getName()
            );

            try {
                messagingService.sendMessage(user.getPhoneNumber(), message);
                log.info("Sent inactivity ping to {}", user.getPhoneNumber());
            } catch (Exception e) {
                log.error("Failed to send inactivity ping to {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }
        log.info("Finished Inactivity Ping job. Pinged {} users.", inactiveUsers.size());
    }
}
