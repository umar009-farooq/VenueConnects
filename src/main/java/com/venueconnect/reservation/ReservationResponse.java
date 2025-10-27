package com.venueconnect.reservation;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReservationResponse {
    private String reservationId; // This will be the key we use in Redis
    private Long eventId;
    private List<Long> eventSeatIds;
    private String status;
    private Instant expiresAt; // When the reservation TTL expires
}