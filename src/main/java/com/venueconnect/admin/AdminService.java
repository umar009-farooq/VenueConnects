package com.venueconnect.admin;

import com.venueconnect.event.Event;
import com.venueconnect.event.EventRepository;
import com.venueconnect.event.TicketType;
import com.venueconnect.event.dto.CreateEventRequest;
import com.venueconnect.event.dto.EventResponse;
import com.venueconnect.event.dto.TicketTypeResponse;
import com.venueconnect.order.Order;
import com.venueconnect.order.OrderRepository;
import com.venueconnect.order.OrderResponse; // Assuming you want to return OrderResponse
import com.venueconnect.order.OrderItemResponse; // Import necessary DTO
import com.venueconnect.venue.Hall;
import com.venueconnect.venue.HallRepository;
import com.venueconnect.venue.Venue; // Import Venue
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EventRepository eventRepository;
    private final HallRepository hallRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        // Find the hall
        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new EntityNotFoundException("Hall not found: " + request.getHallId()));

        // Create the event entity
        Event newEvent = Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .hall(hall)
                .build();

        // Create ticket types and associate them with the event
        List<TicketType> ticketTypes = request.getTicketTypes().stream()
                .map(ttReq -> TicketType.builder()
                        .name(ttReq.getName())
                        .price(ttReq.getPrice())
                        .totalQuantity(ttReq.getTotalQuantity())
                        .event(newEvent) // Link back to the event
                        .build())
                .collect(Collectors.toList());
        newEvent.setTicketTypes(ticketTypes);

        // Save the event (TicketTypes will be saved due to cascade)
        Event savedEvent = eventRepository.save(newEvent);

        // Map to response DTO (similar to EventService)
        return mapToEventResponse(savedEvent);
    }

    // You would add updateEvent logic here too (similar to create, but fetch first)
    // public EventResponse updateEvent(Long eventId, CreateEventRequest request) { ... }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToOrderResponse) // Reuse or create a mapping method
                .collect(Collectors.toList());
    }

    // --- Helper Mappers (Can be moved to a dedicated Mapper class later) ---

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

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .eventSeatId(item.getEventSeat().getId())
                        .seatRow(item.getEventSeat().getSeat().getSeatRow())
                        .seatNumber(item.getEventSeat().getSeat().getSeatNumber())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}