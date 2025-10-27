package com.venueconnect.event;

import com.venueconnect.venue.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {

    // Find all available seats for a specific event
    List<EventSeat> findByEventIdAndStatus(Long eventId, SeatStatus status);

    // Find a specific seat for a specific event
    Optional<EventSeat> findByEventIdAndSeatId(Long eventId, Long seatId);
    List<EventSeat> findByReservationId(String reservationId);
}
