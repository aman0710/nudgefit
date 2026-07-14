package com.nudgefit.scheduler;

import com.nudgefit.model.entity.DailyLog;
import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.repository.DailyLogRepository;
import com.nudgefit.repository.UserRepository;
import com.nudgefit.service.WhatsAppMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EveningSummaryJob {

    private final UserRepository userRepository;
    private final DailyLogRepository dailyLogRepository;
    private final WhatsAppMessagingService messagingService;

    // Runs every day at 10:00 PM server time
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Kolkata")
    public void sendEveningSummary() {
        log.info("Starting Evening Summary job...");
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getConversationState() == ConversationState.ACTIVE)
                .toList();

        for (User user : activeUsers) {
            Optional<DailyLog> todayLog = dailyLogRepository.findByUserIdAndLogDate(user.getId(), LocalDate.now());
            
            String message;
            if (todayLog.isPresent()) {
                DailyLog logData = todayLog.get();
                message = String.format(
                        "Evening %s! 🌙 Here's your summary for today:\n\n" +
                        "🍽️ Consumed: %s kcal\n" +
                        "🏃 Burned: %s kcal\n" +
                        "⚖️ Net: %s kcal (Target: %s kcal)\n\n" +
                        "Great job tracking today! See you tomorrow! 💪",
                        user.getName(),
                        logData.getTotalCaloriesConsumed(),
                        logData.getTotalCaloriesBurned(),
                        logData.getNetCalories(),
                        logData.getTargetCalories()
                );
            } else {
                message = String.format(
                        "Evening %s! 🌙 I noticed you didn't log anything today. " +
                        "Don't worry, every day is a new opportunity. " +
                        "Let's get back on track tomorrow! 💪",
                        user.getName()
                );
            }

            try {
                messagingService.sendMessage(user.getPhoneNumber(), message);
                log.info("Sent evening summary to {}", user.getPhoneNumber());
            } catch (Exception e) {
                log.error("Failed to send evening summary to {}: {}", user.getPhoneNumber(), e.getMessage());
            }
        }
        log.info("Finished Evening Summary job.");
    }
}
