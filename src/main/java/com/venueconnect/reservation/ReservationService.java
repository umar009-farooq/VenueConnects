package com.venueconnect.reservation;

import com.venueconnect.event.EventSeat;
import com.venueconnect.event.EventSeatRepository;
import com.venueconnect.user.User;
import com.venueconnect.venue.SeatStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final EventSeatRepository eventSeatRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public static final long RESERVATION_TTL_MINUTES = 15;
    private final String REDIS_KEY_PREFIX = "Reservation:";

    @Transactional // This method writes to the database, so it's transactional
    public ReservationResponse createReservation(ReservationRequest request) {

        // 1. Get the currently authenticated user
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Generate the unique reservation ID *first*
        String reservationId = UUID.randomUUID().toString();

        // 3. Validate and "tag" the requested seats in Postgres
        for (Long eventSeatId : request.getEventSeatIds()) {
            EventSeat eventSeat = eventSeatRepository.findById(eventSeatId)
                    .orElseThrow(() -> new EntityNotFoundException("EventSeat not found: " + eventSeatId));

            if (!eventSeat.getEvent().getId().equals(request.getEventId())) {
                throw new IllegalArgumentException("Seat " + eventSeatId + " does not belong to event " + request.getEventId());
            }

            if (eventSeat.getStatus() != SeatStatus.AVAILABLE) {
                throw new IllegalStateException("Seat " + eventSeatId + " is not available.");
            }

            // 4. THIS IS THE NEW LOGIC
            eventSeat.setStatus(SeatStatus.RESERVED);
            eventSeat.setReservationId(reservationId); // Tag the seat with the reservationId
            eventSeatRepository.save(eventSeat);
        }

        // 5. Create the "timer" object for Redis
        Reservation reservation = Reservation.builder()
                .id(reservationId)
                .userId(currentUser.getId())
                .eventId(request.getEventId())
                .status("PENDING")
                .createdAt(Instant.now())
                .build(); // Note: We no longer add eventSeatIds here

        // 6. Save to Redis with a 15-minute TTL
        String redisKey = REDIS_KEY_PREFIX + reservationId;
        redisTemplate.opsForValue().set(redisKey, reservation, RESERVATION_TTL_MINUTES, TimeUnit.MINUTES);

        Instant expiresAt = Instant.now().plus(RESERVATION_TTL_MINUTES, TimeUnit.MINUTES.toChronoUnit());

        return ReservationResponse.builder()
                .reservationId(reservationId)
                .eventId(request.getEventId())
                .eventSeatIds(request.getEventSeatIds())
                .status(reservation.getStatus())
                .expiresAt(expiresAt)
                .build();
    }
}