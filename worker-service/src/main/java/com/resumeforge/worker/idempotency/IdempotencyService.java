package com.resumeforge.worker.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * ✅ PHASE 1.4 FIX: Kafka Message Idempotency Service
 *
 * Prevents duplicate processing of Kafka messages using Redis.
 *
 * Problem: Kafka can deliver the same message twice (at-least-once delivery).
 * Without idempotency, duplicate messages cause:
 *   - Duplicate API calls to LLM (wasted API quota)
 *   - Duplicate database records
 *   - Incorrect ATS scores (processed twice)
 *   - Race conditions on status updates
 *
 * Solution: Store processed message IDs in Redis with TTL.
 * Before processing: Check if message ID exists in Redis.
 * If exists: Skip processing (already done).
 * If not: Process message and store ID in Redis.
 *
 * TTL is set to 24 hours to handle message replays within a day.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String KEY_PREFIX = "kafka:idempotent:";
    private static final Duration IDEMPOTENCY_WINDOW = Duration.ofHours(24);

    private final RedisTemplate<String, String> redisTemplate;

    public IdempotencyService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a message has already been processed.
     *
     * @param tailoredResumeId The ID of the tailored resume being processed
     * @return true if message was already processed, false if new
     */
    public boolean isDuplicate(UUID tailoredResumeId) {
        String key = KEY_PREFIX + tailoredResumeId;
        String value = redisTemplate.opsForValue().get(key);
        boolean isDupe = value != null;

        if (isDupe) {
            log.warn("[IDEMPOTENCY] Duplicate message detected: {}", tailoredResumeId);
        }

        return isDupe;
    }

    /**
     * Marks a message as processed in Redis.
     * Stores the ID with a 24-hour TTL to prevent replays.
     *
     * @param tailoredResumeId The ID of the tailored resume being processed
     * @param resultData Optional data to store (e.g., ATS score for quick reference)
     */
    public void markAsProcessed(UUID tailoredResumeId, String resultData) {
        String key = KEY_PREFIX + tailoredResumeId;
        String value = Objects.requireNonNullElse(resultData, tailoredResumeId.toString());

        redisTemplate.opsForValue().set(key, value, IDEMPOTENCY_WINDOW);
        log.debug("[IDEMPOTENCY] Marked message as processed: {} with TTL: {}",
                  tailoredResumeId, IDEMPOTENCY_WINDOW);
    }

    /**
     * Marks a message as failed (won't retry indefinitely).
     * Stores failure marker for debugging and monitoring.
     *
     * @param tailoredResumeId The ID of the tailored resume that failed
     * @param errorMessage Description of the failure
     */
    public void markAsFailed(UUID tailoredResumeId, String errorMessage) {
        String key = KEY_PREFIX + "failed:" + tailoredResumeId;
        String value = errorMessage != null ? errorMessage : "FAILED";

        redisTemplate.opsForValue().set(key, value, IDEMPOTENCY_WINDOW);
        log.debug("[IDEMPOTENCY] Marked message as failed: {} with error: {}",
                  tailoredResumeId, errorMessage);
    }

    /**
     * Checks if a message previously failed.
     * Useful for avoiding infinite retry loops on permanently broken messages.
     *
     * @param tailoredResumeId The ID of the tailored resume
     * @return true if message failed before, false otherwise
     */
    public boolean hasFailed(UUID tailoredResumeId) {
        String key = KEY_PREFIX + "failed:" + tailoredResumeId;
        String value = redisTemplate.opsForValue().get(key);
        return value != null;
    }

    /**
     * Clears the idempotency cache for a message.
     * Use cautiously: allows the message to be reprocessed.
     *
     * @param tailoredResumeId The ID of the tailored resume
     */
    public void clearProcessingState(UUID tailoredResumeId) {
        String key = KEY_PREFIX + tailoredResumeId;
        String failedKey = KEY_PREFIX + "failed:" + tailoredResumeId;

        redisTemplate.delete(key);
        redisTemplate.delete(failedKey);
        log.debug("[IDEMPOTENCY] Cleared processing state for: {}", tailoredResumeId);
    }
}
