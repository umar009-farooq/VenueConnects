package com.venueconnect.reservation;

import com.venueconnect.event.EventSeat;
import com.venueconnect.event.EventSeatRepository;
import com.venueconnect.venue.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j // This is for logging
public class ReservationCleanupService {

    private final EventSeatRepository eventSeatRepository;

    @Transactional
    public void releaseSeats(String reservationId) {
        log.info("Reservation {} expired. Releasing seats.", reservationId);

        // 1. Find all seats in Postgres tagged with this reservationId
        List<EventSeat> seatsToRelease = eventSeatRepository.findByReservationId(reservationId);

        if (seatsToRelease.isEmpty()) {
            log.warn("No seats found for expired reservation {}. They may have been booked.", reservationId);
            return;
        }

        // 2. Loop through and set them back to AVAILABLE
        for (EventSeat seat : seatsToRelease) {
            // We double-check they are still RESERVED, just to be safe
            if (seat.getStatus() == SeatStatus.RESERVED) {
                seat.setStatus(SeatStatus.AVAILABLE);
                seat.setReservationId(null); // Clean up the tag
                eventSeatRepository.save(seat);
            }
        }

        log.info("Successfully released {} seats for reservation {}", seatsToRelease.size(), reservationId);
    }
}