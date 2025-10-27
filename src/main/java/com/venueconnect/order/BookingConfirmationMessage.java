package com.venueconnect.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Simple DTO for the RabbitMQ message body
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationMessage {
    private Long orderId;
    private String reservationId; // Include this in case the worker needs it
}