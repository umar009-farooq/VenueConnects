package com.venueconnect.kafka;

import com.venueconnect.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// A generic payload for order-related events
@Data
@Builder
@NoArgsConstructor // <-- ADD THIS
@AllArgsConstructor
public class OrderEventPayload {
    private Long orderId;
    private Long userId;
    private String reservationId;
    private OrderStatus orderStatus;
    private BigDecimal totalAmount;
    private LocalDateTime timestamp;
    private List<Long> eventSeatIds; // List of seat IDs involved
}