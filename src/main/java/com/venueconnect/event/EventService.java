package com.venueconnect.event;
import com.venueconnect.event.dto.EventResponse;
import com.venueconnect.event.dto.TicketTypeResponse;
import com.venueconnect.venue.Hall;
import com.venueconnect.venue.Venue;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    @Transactional(readOnly = true) // Use read-only transactions for 'GET' operations
    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(this::mapToEventResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + id));
        return mapToEventResponse(event);
    }

    // Helper method to map Event Entity to EventResponse DTO
    private EventResponse mapToEventResponse(Event event) {
        Hall hall = event.getHall();
        Venue venue = hall.getVenue();

        List<TicketTypeResponse> ticketTypeResponses = event.getTicketTypes().stream()
                .map(ticketType -> TicketTypeResponse.builder()
                        .id(ticketType.getId())
                        .name(ticketType.getName())
                        .price(ticketType.getPrice())
                        .totalQuantity(ticketType.getTotalQuantity())
                        .build())
                .collect(Collectors.toList());

        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .venueName(venue.getName())
                .hallName(hall.getName())
                .city(venue.getCity())
                .ticketTypes(ticketTypeResponses)
                .build();
    }
}