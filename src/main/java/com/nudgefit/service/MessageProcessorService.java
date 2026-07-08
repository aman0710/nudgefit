package com.nudgefit.service;

import com.nudgefit.model.dto.MessageReceivedEvent;
import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessorService {

    private final UserRepository userRepository;
    private final ConversationContextService contextService;
    private final OnboardingService onboardingService;
    private final IntentClassifierService intentClassifierService;
    private final FoodLoggingService foodLoggingService;
    private final WorkoutLoggingService workoutLoggingService;
    private final CoachResponseService coachResponseService;
    private final GoalEngineService goalEngineService;
    private final WhatsAppMessagingService messagingService;

    @Async
    @EventListener
    public void handleMessageReceivedEvent(MessageReceivedEvent event) {
        String phoneNumber = event.phoneNumber();
        String messageBody = event.messageBody();

        try {
            // Load or create user
            User user = userRepository.findByPhoneNumber(phoneNumber).orElseGet(() -> {
                User newUser = User.builder()
                        .phoneNumber(phoneNumber)
                        .conversationState(ConversationState.NEW_USER)
                        .build();
                return userRepository.save(newUser);
            });

            // Update last message timestamp
            user.setLastMessageAt(LocalDateTime.now());
            userRepository.save(user);

            // Add user message to context
            contextService.addMessage(phoneNumber, "user", messageBody);

            ConversationState state = user.getConversationState();
            String responseText;

            if (state == null || state != ConversationState.ACTIVE) {
                // Route to onboarding
                responseText = onboardingService.handleOnboarding(user, messageBody);
            } else {
                // Route to intent classification
                com.nudgefit.model.dto.IntentClassificationResponse classification = intentClassifierService.classifyIntent(phoneNumber, messageBody);
                
                responseText = switch (classification.intent()) {
                    case FOOD_LOG -> foodLoggingService.logFood(user, messageBody);
                    case WORKOUT_LOG -> workoutLoggingService.logWorkout(user, messageBody);
                    case PROGRESS_CHECK -> coachResponseService.generateCoachingResponse(user, messageBody, null, null); // Provide stats
                    default -> coachResponseService.generateCoachingResponse(user, messageBody, null, null); // General chat
                };
            }

            // Send response back
            messagingService.sendMessage(phoneNumber, responseText);
            
            // Add bot message to context
            contextService.addMessage(phoneNumber, "assistant", responseText);

        } catch (IllegalArgumentException e) {
            log.warn("Validation error for {}: {}", phoneNumber, e.getMessage());
            messagingService.sendMessage(phoneNumber, e.getMessage());
        } catch (Exception e) {
            log.error("Error processing message for {}: {}", phoneNumber, e.getMessage(), e);
            messagingService.sendMessage(phoneNumber, "I'm having a bit of trouble right now. Can you try again in a minute? 🙏");
        }
    }
}
