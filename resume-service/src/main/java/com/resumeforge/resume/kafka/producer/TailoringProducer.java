package com.resumeforge.resume.kafka.producer;

import com.resumeforge.resume.kafka.event.TailoringRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TailoringProducer {

    private static final String TOPIC = "resume.tailoring.requested";
    private final KafkaTemplate<String, TailoringRequestedEvent> kafkaTemplate;

    public void publish(TailoringRequestedEvent event) {
        log.info("Publishing tailoring request for resumeId={}", event.getTailoredResumeId());
        kafkaTemplate.send(TOPIC, event.getTailoredResumeId().toString(), event);
    }
}
