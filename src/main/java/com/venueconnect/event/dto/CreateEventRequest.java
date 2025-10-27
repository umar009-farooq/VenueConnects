package com.venueconnect.event.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long hallId; // ID of the hall where the event takes place
    private List<CreateTicketTypeRequest> ticketTypes;
}