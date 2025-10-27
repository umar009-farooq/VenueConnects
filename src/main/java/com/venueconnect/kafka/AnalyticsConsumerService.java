package com.venueconnect.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnalyticsConsumerService {

    // Define the topic name and a group ID for the consumer
    private static final String ANALYTICS_TOPIC = "events.analytics";
    private static final String GROUP_ID = "venueconnect-analytics-group";

    /**
     * Listens for messages on the analytics topic.
     * @param message The message payload (deserialized from JSON). We expect OrderEventPayload here.
     * @param topic The topic the message came from.
     * @param partition The partition the message came from.
     */
    @KafkaListener(topics = ANALYTICS_TOPIC, groupId = GROUP_ID)
    public void consumeAnalyticsEvent(
            @Payload OrderEventPayload message, // Specify the expected payload type
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        log.info("Received Analytics Event: Partition={}, Topic={}, Payload={}",
                partition, topic, message);

        // --- TODO: Add real analytics processing logic ---
        // Examples:
        // - Save to a database (e.g., a NoSQL DB like MongoDB for analytics)
        // - Send to an external analytics service (e.g., Mixpanel, Google Analytics)
        // - Update dashboards
        // For now, we just log it.
        // --- End TODO ---
    }
}