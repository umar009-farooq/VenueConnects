package com.venueconnect.reservation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

// This DTO is for storing the reservation in Redis
@Data
@Builder
@NoArgsConstructor  // <-- ADD THIS
@AllArgsConstructor
@RedisHash("Reservation") // This tells Spring Data Redis to store it as a Hash
public class Reservation implements Serializable {

    @Id
    private String id; // The unique reservation ID (e.g., a UUID)

    @Indexed // Allows us to find reservations by user ID
    private Long userId;

    private Long eventId;
//    private List<Long> eventSeatIds; // The IDs of the EventSeat entities
    private String status; // e.g., "PENDING"
    private Instant createdAt;
}