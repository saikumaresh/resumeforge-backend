package com.resumeforge.resume.kafka.producer;

import com.resumeforge.resume.kafka.event.TailoringRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TailoringProducer {

    private static final Logger log = LoggerFactory.getLogger(TailoringProducer.class);
    private static final String TOPIC = "resume.tailoring.requested";

    private final KafkaTemplate<String, TailoringRequestedEvent> kafkaTemplate;

    public TailoringProducer(KafkaTemplate<String, TailoringRequestedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public boolean publish(TailoringRequestedEvent event) {
        log.info("Publishing tailoring request for resumeId={}", event.getTailoredResumeId());
        try {
            kafkaTemplate.send(TOPIC, event.getTailoredResumeId().toString(), event);
            log.info("Kafka message queued for resumeId={}", event.getTailoredResumeId());
            return true;
        } catch (Exception e) {
            log.error("Failed to publish Kafka event for resumeId={}: {} — {}",
                    event.getTailoredResumeId(), e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}
