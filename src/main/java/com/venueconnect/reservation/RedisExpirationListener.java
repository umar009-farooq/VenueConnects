package com.venueconnect.reservation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener; // Import this
import org.springframework.stereotype.Component;

@Component // Make sure it's a Spring component
@Slf4j
@RequiredArgsConstructor // Use constructor injection
public class RedisExpirationListener implements MessageListener { // Implement MessageListener directly

    private final ReservationCleanupService cleanupService;
    private final String REDIS_KEY_PREFIX = "Reservation:";

    /**
     * This method will be called by the MessageListenerAdapter when an expiration event occurs.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        log.debug("Received expired key event: {}", expiredKey);

        // We only care about our "Reservation" keys
        if (expiredKey.startsWith(REDIS_KEY_PREFIX)) {
            // Extract the reservationId
            String reservationId = expiredKey.substring(REDIS_KEY_PREFIX.length());

            // Call our service to clean up the seats
            try { // Add try-catch for better error logging
                cleanupService.releaseSeats(reservationId);
            } catch (Exception e) {
                log.error("Error releasing seats for expired reservation {}: {}", reservationId, e.getMessage(), e);
            }
        }
    }
}