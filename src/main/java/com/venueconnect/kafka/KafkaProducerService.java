package com.venueconnect.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    // KafkaTemplate is automatically configured by Spring Boot
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Define topic names based on the plan [cite: 55]
    private static final String AUDIT_TOPIC = "events.audit";
    private static final String ANALYTICS_TOPIC = "events.analytics";

    /**
     * Sends an audit-related event to the audit topic.
     * @param event The event payload (will be serialized to JSON).
     */
    public void sendAuditEvent(Object event) {
        log.info("Sending audit event to topic {}: {}", AUDIT_TOPIC, event);
        try {
            // Send the event. The key is null, value is the event object.
            kafkaTemplate.send(AUDIT_TOPIC, event);
        } catch (Exception e) {
            log.error("Error sending audit event to Kafka: {}", e.getMessage(), e);
            // Handle exceptions (e.g., logging, metrics)
        }
    }

    /**
     * Sends an analytics-related event to the analytics topic.
     * @param event The event payload (will be serialized to JSON).
     */
    public void sendAnalyticsEvent(Object event) {
        log.info("Sending analytics event to topic {}: {}", ANALYTICS_TOPIC, event);
        try {
            // Send the event. The key is null, value is the event object.
            kafkaTemplate.send(ANALYTICS_TOPIC, event);
        } catch (Exception e) {
            log.error("Error sending analytics event to Kafka: {}", e.getMessage(), e);
            // Handle exceptions
        }
    }
}