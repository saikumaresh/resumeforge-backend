package com.resumeforge.worker.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Component
public class OllamaClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    @Value("${ollama.api-url}")
    private String apiUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String tailorResume(String masterResumeContent, String jobDescriptionContent) {
        log.info("Calling Ollama API for resume tailoring with model={}", model);
        String prompt = buildPrompt(masterResumeContent, jobDescriptionContent);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content",
                        "You are a professional resume writer. Always respond with valid JSON only. No markdown, no explanation."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("stream", false);
        requestBody.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.warn("Null response from Ollama, using fallback");
                return generateFallbackResume(masterResumeContent);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            log.info("Ollama response received successfully");
            return stripCodeFences(content);
        } catch (Exception e) {
            log.error("Ollama API call failed: {}. Using fallback.", e.getMessage());
            return generateFallbackResume(masterResumeContent);
        }
    }

    private String buildPrompt(String masterContent, String jdContent) {
        return """
            Tailor this resume for the job description below.

            MASTER RESUME:
            %s

            JOB DESCRIPTION:
            %s

            Respond ONLY with this JSON structure (no markdown, no extra text):
            {
              "summary": "tailored professional summary matching job requirements",
              "experience": "tailored experience section emphasising relevant achievements",
              "skills": "comma-separated relevant skills from both resume and job description",
              "education": "education section",
              "projects": "relevant projects"
            }
            """.formatted(masterContent, jdContent);
    }

    private String stripCodeFences(String content) {
        if (content == null) return "{}";
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceFirst("```[a-zA-Z]*\\n?", "");
            content = content.replaceAll("```$", "").trim();
        }
        return content;
    }

    private String generateFallbackResume(String masterContent) {
        String truncated = masterContent != null
            ? masterContent.replace("\"", "'").substring(0, Math.min(200, masterContent.length()))
            : "Experienced professional";
        return """
            {
              "summary": "Experienced professional seeking this role with strong relevant background.",
              "experience": "%s",
              "skills": "Java, Spring Boot, Kafka, Docker, PostgreSQL",
              "education": "Available on request",
              "projects": "See full portfolio on GitHub"
            }
            """.formatted(truncated);
    }
}
