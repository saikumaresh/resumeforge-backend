package com.resumeforge.worker.llm;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the HTTP call to the Ollama chat-completions endpoint, and nothing else.
 *
 * This exists as a separate bean for a specific reason. Resilience4j's
 * {@code @CircuitBreaker} and {@code @Retry} are applied by a Spring AOP proxy,
 * and a proxy can only intercept a call that arrives through it — that is, a
 * public method invoked from outside the bean. Previously these annotations sat
 * on a private method that {@code OllamaClient} called on itself, so the proxy
 * was bypassed entirely and neither the circuit breaker nor the retry policy
 * ever executed. Moving the annotated method onto its own bean, invoked by a
 * different object, is what makes them take effect.
 */
@Component
public class OllamaApiCaller {

    private static final Logger log = LoggerFactory.getLogger(OllamaApiCaller.class);

    @Value("${ollama.api-url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.api-key:}")
    private String apiKey;

    /** Finite timeouts: an unresponsive endpoint must not pin the consumer thread. */
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(120))
            .build();

    /**
     * Performs the chat-completion request. Public and invoked from another bean
     * so that the Resilience4j proxy applies.
     *
     * @throws OllamaUnavailableException when the endpoint is unreachable, the
     *         circuit is open, or the response cannot be interpreted
     */
    @CircuitBreaker(name = "ollama-api", fallbackMethod = "handleFailure")
    @Retry(name = "ollama-api", fallbackMethod = "handleFailure")
    public String call(String systemPrompt, String userPrompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
        ));
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.2);   // factual accuracy over creativity
        requestBody.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl, new HttpEntity<>(requestBody, headers), Map.class);

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new OllamaUnavailableException("Ollama returned an empty response body");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new OllamaUnavailableException("Ollama response contained no choices");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        if (message == null || message.get("content") == null) {
            throw new OllamaUnavailableException("Ollama response contained no message content");
        }

        log.info("[OLLAMA] Response received");
        return (String) message.get("content");
    }

    /**
     * Invoked when the retries are exhausted or the circuit is open. It converts
     * whatever went wrong into a single exception type; it deliberately does not
     * synthesise substitute content, because a caller must be able to tell a real
     * tailoring result from a failed one.
     */
    @SuppressWarnings("unused") // referenced by name from the annotations above
    public String handleFailure(String systemPrompt, String userPrompt, Throwable t) {
        if (t instanceof OllamaUnavailableException oue) {
            throw oue;
        }
        log.warn("[CIRCUIT-BREAKER] Ollama call failed ({}): {}",
                t.getClass().getSimpleName(), t.getMessage());
        throw new OllamaUnavailableException("Ollama service temporarily unavailable", t);
    }
}
