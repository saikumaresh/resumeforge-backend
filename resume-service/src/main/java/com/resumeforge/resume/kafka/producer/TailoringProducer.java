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

    public void publish(TailoringRequestedEvent event) {
        log.info("Publishing tailoring request for resumeId={}", event.getTailoredResumeId());
        kafkaTemplate.send(TOPIC, event.getTailoredResumeId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("KAFKA DELIVERY FAILED for resumeId={}: {}",
                        event.getTailoredResumeId(), ex.getMessage());
                } else {
                    log.debug("Kafka delivery confirmed for resumeId={} partition={} offset={}",
                        event.getTailoredResumeId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
                }
            });
    }
}
