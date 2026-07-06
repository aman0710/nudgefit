package com.nudgefit.scheduler;

import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.repository.UserRepository;
import com.nudgefit.service.WhatsAppMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MorningCheckInJob {

    private final UserRepository userRepository;
    private final WhatsAppMessagingService messagingService;

    // Runs every day at 8:00 AM server time
    @Scheduled(cron = "0 0 8 * * *")
    public void sendMorningCheckIn() {
        log.info("Starting Morning Check-in job...");
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getConversationState() == ConversationState.ACTIVE)
                .toList();

        for (User user : activeUsers) {
            String message = String.format(
                    "Good morning %s! ☀️ Ready to crush it today?\n\n" +
                    "Your daily targets:\n" +
                    "🔥 %s kcal\n" +
                    "🥩 Protein: %sg\n" +
                    "🍚 Carbs: %sg\n" +
                    "🥑 Fats: %sg\n\n" +
                    "Don't forget to log your meals and workouts!",
                    user.getName(),
                    user.getDailyCalorieTarget(),
                    user.getDailyProteinTargetG(),
                    user.getDailyCarbsTargetG(),
                    user.getDailyFatTargetG()
            );

            try {
                messagingService.sendMessage(user.getPhoneNumber(), message);
                log.info("Sent morning check-in to {}", user.getPhoneNumber());
            } catch (Exception e) {
                log.error("Failed to send morning check-in to {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }
        log.info("Finished Morning Check-in job.");
    }
}
