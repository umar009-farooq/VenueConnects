package com.venueconnect.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private Long eventId;
    private List<Long> eventSeatIds; // The list of seats the user wants to reserve
}