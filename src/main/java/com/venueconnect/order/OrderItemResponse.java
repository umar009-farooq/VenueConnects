package com.venueconnect.order;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private Long eventSeatId;
    private String seatRow;
    private String seatNumber;
    private BigDecimal price;
}