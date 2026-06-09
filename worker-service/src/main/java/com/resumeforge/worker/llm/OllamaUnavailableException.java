package com.resumeforge.worker.llm;

/**
 * ✅ PHASE 1.5 FIX: Custom exception for Ollama API unavailability.
 *
 * Thrown when:
 * - Ollama service is unreachable
 * - Circuit breaker is open (service degraded)
 * - Connection timeout
 * - Too many failures
 *
 * This allows the system to fall back to generic tailoring
 * without repeatedly trying to reach an unavailable service.
 */
public class OllamaUnavailableException extends RuntimeException {

    public OllamaUnavailableException(String message) {
        super(message);
    }

    public OllamaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
