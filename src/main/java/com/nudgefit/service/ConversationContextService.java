package com.nudgefit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nudgefit.model.entity.User;
import com.nudgefit.model.enums.ConversationState;
import com.nudgefit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Manages conversation state and context in Redis with DB fallback.
 * See blueprint section 10 for key patterns and TTLs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationContextService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String STATE_KEY = "user:state:%s";
    private static final String DAILY_KEY = "user:daily:%s:%s";
    private static final String MESSAGES_KEY = "user:messages:%s";
    private static final String PROFILE_KEY = "user:profile:%s";
    private static final int MAX_MESSAGES = 20;

    // ── State Management ──

    public ConversationState getState(String phoneNumber) {
        String key = String.format(STATE_KEY, phoneNumber);
        String state = redisTemplate.opsForValue().get(key);

        if (state != null) {
            return ConversationState.valueOf(state);
        }

        // Fallback to DB
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(User::getConversationState)
                .orElse(null);
    }

    public void setState(String phoneNumber, ConversationState state) {
        String key = String.format(STATE_KEY, phoneNumber);
        redisTemplate.opsForValue().set(key, state.name());

        // Also update in DB
        userRepository.findByPhoneNumber(phoneNumber).ifPresent(user -> {
            user.setConversationState(state);
            userRepository.save(user);
        });
    }

    // ── Message History ──

    public void addMessage(String phoneNumber, String role, String content) {
        try {
            String key = String.format(MESSAGES_KEY, phoneNumber);
            Map<String, String> message = Map.of(
                    "role", role,
                    "content", content,
                    "timestamp", java.time.LocalDateTime.now().toString()
            );
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().leftPush(key, json);
            redisTemplate.opsForList().trim(key, 0, MAX_MESSAGES - 1);
        } catch (Exception e) {
            log.error("Failed to add message to Redis for {}: {}", phoneNumber, e.getMessage());
        }
    }

    public List<String> getRecentMessages(String phoneNumber, int count) {
        String key = String.format(MESSAGES_KEY, phoneNumber);
        List<String> messages = redisTemplate.opsForList().range(key, 0, count - 1);
        return messages != null ? messages : Collections.emptyList();
    }

    // ── Daily Totals ──

    public void updateDailyTotals(String phoneNumber, String date, String consumed, String burned) {
        String key = String.format(DAILY_KEY, phoneNumber, date);
        redisTemplate.opsForHash().put(key, "consumed", consumed);
        redisTemplate.opsForHash().put(key, "burned", burned);
        redisTemplate.expire(key, Duration.ofHours(24));
    }

    public Map<Object, Object> getDailyTotals(String phoneNumber, String date) {
        String key = String.format(DAILY_KEY, phoneNumber, date);
        return redisTemplate.opsForHash().entries(key);
    }

    // ── User Profile Cache ──

    public void cacheUserProfile(String phoneNumber, User user) {
        try {
            String key = String.format(PROFILE_KEY, phoneNumber);
            String json = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(key, json, Duration.ofHours(1));
        } catch (Exception e) {
            log.error("Failed to cache user profile for {}: {}", phoneNumber, e.getMessage());
        }
    }

    public User getCachedUserProfile(String phoneNumber) {
        try {
            String key = String.format(PROFILE_KEY, phoneNumber);
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, User.class);
            }
        } catch (Exception e) {
            log.error("Failed to read cached user profile for {}: {}", phoneNumber, e.getMessage());
        }
        return null;
    }

    /**
     * Invalidates cached user profile so next read fetches from DB.
     */
    public void invalidateUserProfile(String phoneNumber) {
        String key = String.format(PROFILE_KEY, phoneNumber);
        redisTemplate.delete(key);
    }
}
