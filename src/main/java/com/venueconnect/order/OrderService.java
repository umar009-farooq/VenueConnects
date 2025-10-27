package com.venueconnect.order;

// ... other imports ...
import com.venueconnect.config.RabbitMQConfig;
import com.venueconnect.event.EventSeat;
import com.venueconnect.event.EventSeatRepository;
import com.venueconnect.kafka.KafkaProducerService;
import com.venueconnect.kafka.OrderEventPayload;
import com.venueconnect.reservation.Reservation;
import com.venueconnect.user.Role; // Import Role
import com.venueconnect.user.User;
import com.venueconnect.venue.SeatStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.AccessDeniedException; // Import AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Ensure LocalDateTime is imported
import java.util.List;
import java.util.stream.Collectors;

// ... (existing class content) ...

@Service
@RequiredArgsConstructor
public class OrderService {

    // ... (existing fields: repositories, templates, kafka service) ...
    private final OrderRepository orderRepository;
    private final EventSeatRepository eventSeatRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final KafkaProducerService kafkaProducerService;

    private final String REDIS_KEY_PREFIX = "Reservation:";


    // ... (existing createOrder method) ...
    @Transactional
    public OrderResponse createOrder(PaymentRequest request) {

        // 1. Get user
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Fetch reservation from REDIS
        String redisKey = REDIS_KEY_PREFIX + request.getReservationId();
        Reservation reservation = (Reservation) redisTemplate.opsForValue().get(redisKey);

        // 3. Validate reservation
        if (reservation == null) {
            throw new EntityNotFoundException("Reservation not found or has expired: " + request.getReservationId());
        }
        if (!reservation.getUserId().equals(currentUser.getId())) {
            throw new IllegalStateException("This reservation does not belong to the current user.");
        }

        // 4. Mock payment
        boolean paymentSuccessful = mockPayment(request.getPaymentMethod());
        if (!paymentSuccessful) {
            // If payment fails, we just throw an exception. The Redis key will expire eventually.
            // We could optionally delete the Redis key here if needed.
            throw new RuntimeException("Payment failed.");
        }

        // --- START OF REFACTORED LOGIC ---

        // 5. Fetch seats from POSTGRES (using reservationId) just to calculate the total amount
        List<EventSeat> reservedSeats = eventSeatRepository.findByReservationId(request.getReservationId());
        if (reservedSeats.isEmpty()) {
            throw new IllegalStateException("No seats found for this reservation. It may have expired.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Long> eventSeatIds = reservedSeats.stream().map(EventSeat::getId).collect(Collectors.toList()); // Collect seat IDs

        for (EventSeat seat : reservedSeats) {
            // Check if seats are still RESERVED (important!)
            if (seat.getStatus() != SeatStatus.RESERVED) {
                throw new IllegalStateException("Seat " + seat.getId() + " status is not RESERVED. Cannot proceed.");
            }
            totalAmount = totalAmount.add(seat.getTicketType().getPrice());
        }

        // 6. Create the Order in POSTGRES with status PAYMENT_COMPLETE
        Order newOrder = Order.builder()
                .user(currentUser)
                .totalAmount(totalAmount)
                .status(OrderStatus.PAYMENT_COMPLETE) // Use the new status
                .createdAt(LocalDateTime.now())
                .reservationId(request.getReservationId())
                .build();

        // 7. Create OrderItems (linking seats but NOT changing their status yet)
        List<OrderItem> orderItems = reservedSeats.stream()
                .map(seat -> OrderItem.builder()
                        .order(newOrder)
                        .eventSeat(seat)
                        .price(seat.getTicketType().getPrice())
                        .build())
                .collect(Collectors.toList());
        newOrder.setOrderItems(orderItems);

        // 8. Save the Order (and its items)
        Order savedOrder = orderRepository.save(newOrder);

        // 9. Create the message for RabbitMQ
        BookingConfirmationMessage message = new BookingConfirmationMessage(
                savedOrder.getId(),
                savedOrder.getReservationId()
        );

        // 10. Publish the message to the booking exchange
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.BOOKING_EXCHANGE_NAME,
                RabbitMQConfig.BOOKING_ROUTING_KEY,
                message
        );

        // 11. Delete the reservation "timer" from REDIS (payment is done)
        redisTemplate.delete(redisKey);

        // --- END OF REFACTORED LOGIC ---

        // --- SEND KAFKA EVENT ---
        OrderEventPayload kafkaPayload = OrderEventPayload.builder()
                .orderId(savedOrder.getId())
                .userId(currentUser.getId())
                .reservationId(savedOrder.getReservationId())
                .orderStatus(savedOrder.getStatus())
                .totalAmount(savedOrder.getTotalAmount())
                .timestamp(savedOrder.getCreatedAt())
                .eventSeatIds(eventSeatIds)
                .build();

        kafkaProducerService.sendAuditEvent(kafkaPayload); // Send to audit topic
        kafkaProducerService.sendAnalyticsEvent(kafkaPayload); // Send to analytics topic
        // --- END KAFKA EVENT ---


        // 12. Return a response (note status is PAYMENT_COMPLETE)
        return mapToOrderResponse(savedOrder);
    }

    // --- ADD CANCEL ORDER METHOD ---
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        // 1. Get current user
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // 2. Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        // 3. Authorization Check: Allow only the order owner or Admin/Organizer
        boolean isAdminOrOrganizer = currentUser.getRole() == Role.ROLE_ADMIN || currentUser.getRole() == Role.ROLE_ORGANIZER;
        if (!order.getUser().getId().equals(currentUser.getId()) && !isAdminOrOrganizer) {
            throw new AccessDeniedException("User does not have permission to cancel this order.");
        }

        // 4. Validation: Check if the order is in a cancellable state
        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PAYMENT_COMPLETE) {
            throw new IllegalStateException("Order cannot be cancelled. Current status: " + order.getStatus());
        }
        // Optional: Add time-based validation (e.g., cannot cancel within 24 hours of event start time)
        // LocalDateTime eventStartTime = order.getOrderItems().get(0).getEventSeat().getEvent().getStartTime();
        // if (LocalDateTime.now().isAfter(eventStartTime.minusHours(24))) {
        //     throw new IllegalStateException("Cannot cancel order less than 24 hours before the event.");
        // }


        // 5. Update Order Status
        order.setStatus(OrderStatus.CANCELLED);

        // 6. Release the Seats
        List<Long> releasedSeatIds = order.getOrderItems().stream()
                .map(item -> {
                    EventSeat seat = item.getEventSeat();
                    // Only release if it was actually booked by this order
                    if (seat.getStatus() == SeatStatus.BOOKED || seat.getStatus() == SeatStatus.RESERVED) {
                        seat.setStatus(SeatStatus.AVAILABLE);
                        seat.setReservationId(null); // Clear any lingering reservation ID
                        eventSeatRepository.save(seat);
                        return seat.getId();
                    }
                    return null; // Should not happen in CONFIRMED state
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());


        // 7. Mock Refund Process
        mockRefund(order.getId(), order.getTotalAmount());

        // 8. Save the updated order
        Order cancelledOrder = orderRepository.save(order);

        // 9. Send Kafka Event
        OrderEventPayload kafkaPayload = OrderEventPayload.builder()
                .orderId(cancelledOrder.getId())
                .userId(currentUser.getId())
                .reservationId(cancelledOrder.getReservationId())
                .orderStatus(cancelledOrder.getStatus()) // CANCELLED
                .totalAmount(cancelledOrder.getTotalAmount())
                .timestamp(LocalDateTime.now()) // Use current time for cancellation event
                .eventSeatIds(releasedSeatIds)
                .build();
        kafkaProducerService.sendAuditEvent(kafkaPayload); // Audit cancellation
        kafkaProducerService.sendAnalyticsEvent(kafkaPayload); // Analytics

        return mapToOrderResponse(cancelledOrder);
    }

    private void mockRefund(Long orderId, BigDecimal amount) {
        // In a real application, interact with a payment gateway API to issue a refund.
        System.out.println("Mock Refund Initiated: Refunding " + amount + " for Order ID " + orderId);
    }
    // --- END CANCEL ORDER METHOD ---


    // ... (mockPayment and mapToOrderResponse methods remain the same) ...
    private boolean mockPayment(String paymentMethod) {
        return true;
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