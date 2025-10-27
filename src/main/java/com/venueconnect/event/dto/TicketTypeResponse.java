package com.venueconnect.event.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TicketTypeResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
}