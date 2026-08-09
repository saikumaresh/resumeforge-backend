package com.resumeforge.worker.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests the duplicate suppression that protects the tailoring pipeline.
 *
 * Kafka delivers at least once, so a consumer that crashes after calling the
 * model but before committing its offset will receive the same event again.
 * Without this service that redelivery means a second model invocation and a
 * duplicated result, so the behaviour asserted here is what makes the pipeline
 * safe to retry.
 *
 * RedisTemplate is a mock rather than an embedded server: the contract worth
 * asserting is which key is read, which key is written, and with what
 * expiry, all of which are interactions rather than return values.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String PREFIX = "kafka:idempotent:";

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    @Captor private ArgumentCaptor<String> keyCaptor;
    @Captor private ArgumentCaptor<String> valueCaptor;
    @Captor private ArgumentCaptor<Duration> ttlCaptor;

    private IdempotencyService service;
    private UUID id;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(redisTemplate);
        id = UUID.randomUUID();
    }

    @Test
    @DisplayName("an event never seen before is not a duplicate")
    void firstDeliveryIsNotDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(PREFIX + id)).thenReturn(null);

        assertFalse(service.isDuplicate(id));
        verify(valueOps).get(PREFIX + id);
    }

    @Test
    @DisplayName("an event already recorded is reported as a duplicate")
    void redeliveryIsDuplicate() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(PREFIX + id)).thenReturn("previous-result");

        assertTrue(service.isDuplicate(id));
    }

    @Test
    @DisplayName("the lookup key is namespaced by the tailored-resume id")
    void keyIsNamespaced() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        service.isDuplicate(id);

        verify(valueOps).get(keyCaptor.capture());
        assertEquals(PREFIX + id, keyCaptor.getValue(),
                "a shared or unprefixed key would collide with other Redis users");
    }

    @Test
    @DisplayName("marking an event processed writes it under a bounded expiry")
    void markAsProcessedSetsTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service.markAsProcessed(id, "score=84");

        verify(valueOps).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());
        assertEquals(PREFIX + id, keyCaptor.getValue());
        assertEquals("score=84", valueCaptor.getValue());
        assertEquals(Duration.ofHours(24), ttlCaptor.getValue(),
                "an unbounded key would grow the keyspace without limit");
    }

    @Test
    @DisplayName("a null result still records the event, using the id as the value")
    void markAsProcessedWithoutResultData() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        service.markAsProcessed(id, null);

        verify(valueOps).set(anyString(), valueCaptor.capture(), any(Duration.class));
        assertEquals(id.toString(), valueCaptor.getValue(),
                "the marker must be written even when there is no result to store");
    }

    @Test
    @DisplayName("checking for a duplicate performs no write")
    void checkingDoesNotWrite() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        service.isDuplicate(id);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("distinct events occupy distinct keys")
    void distinctEventsDoNotCollide() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        UUID other = UUID.randomUUID();

        service.markAsProcessed(id, "a");
        service.markAsProcessed(other, "b");

        verify(valueOps, times(2)).set(keyCaptor.capture(), anyString(), any(Duration.class));
        assertNotEquals(keyCaptor.getAllValues().get(0), keyCaptor.getAllValues().get(1));
    }
}
