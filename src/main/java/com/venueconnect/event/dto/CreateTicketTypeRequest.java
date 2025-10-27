package com.venueconnect.event.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateTicketTypeRequest {
    private String name;
    private BigDecimal price;
    private Integer totalQuantity;
}