package com.venueconnect.admin;

import com.venueconnect.event.dto.CreateEventRequest;
import com.venueconnect.event.dto.EventResponse;
import com.venueconnect.order.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(@RequestBody CreateEventRequest request) {
        EventResponse createdEvent = adminService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
    }

    // Placeholder for update - Implement in AdminService first
    // @PutMapping("/events/{id}")
    // public ResponseEntity<EventResponse> updateEvent(@PathVariable Long id, @RequestBody CreateEventRequest request) {
    //     EventResponse updatedEvent = adminService.updateEvent(id, request);
    //     return ResponseEntity.ok(updatedEvent);
    // }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(adminService.getAllOrders());
    }
}