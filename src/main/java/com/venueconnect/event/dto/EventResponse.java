package com.venueconnect.event.dto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Details about the venue and hall
    private String venueName;
    private String hallName;
    private String city;

    private List<TicketTypeResponse> ticketTypes;
}