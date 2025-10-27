package com.venueconnect.worker;

import com.venueconnect.config.RabbitMQConfig;
import com.venueconnect.event.EventSeat;
import com.venueconnect.event.EventSeatRepository;
import com.venueconnect.kafka.KafkaProducerService; // <-- Import Kafka service
import com.venueconnect.kafka.OrderEventPayload; // <-- Import event payload DTO
import com.venueconnect.order.BookingConfirmationMessage;
import com.venueconnect.order.Order;
import com.venueconnect.order.OrderRepository;
import com.venueconnect.order.OrderStatus;
import com.venueconnect.venue.SeatStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingWorker {

    private final OrderRepository orderRepository;
    private final EventSeatRepository eventSeatRepository;
    private final KafkaProducerService kafkaProducerService; // <-- Inject Kafka service

    @RabbitListener(queues = RabbitMQConfig.BOOKING_QUEUE_NAME)
    @Transactional
    public void handleBookingConfirmation(BookingConfirmationMessage message) {
        log.info("Received booking confirmation message for Order ID: {}", message.getOrderId());
        Order order = null; // Declare order outside try block for use in catch/finally
        List<EventSeat> seatsToBook = null;

        try {
            order = orderRepository.findById(message.getOrderId())
                    .orElseThrow(() -> new EntityNotFoundException("Order not found: " + message.getOrderId()));

            if (order.getStatus() != OrderStatus.PAYMENT_COMPLETE) {
                log.warn("Order {} is not in PAYMENT_COMPLETE status. Current status: {}. Skipping confirmation.",
                        order.getId(), order.getStatus());
                return;
            }

            seatsToBook = eventSeatRepository.findByReservationId(order.getReservationId());
            if (seatsToBook.isEmpty()) {
                log.error("CRITICAL: No seats found for reservationId {} linked to Order {}. Cannot confirm booking.",
                        order.getReservationId(), order.getId());
                // Consider throwing an exception here for DLQ
                throw new IllegalStateException("No seats found for reservation " + order.getReservationId());
            }

            log.info("Updating {} seats to BOOKED for Order {}", seatsToBook.size(), order.getId());
            for (EventSeat seat : seatsToBook) {
                if (seat.getStatus() != SeatStatus.RESERVED) {
                    log.error("CRITICAL: Seat {} for Order {} was expected to be RESERVED but was {}. Booking may be inconsistent.",
                            seat.getId(), order.getId(), seat.getStatus());
                    throw new IllegalStateException("Seat " + seat.getId() + " status mismatch during booking confirmation.");
                }
                seat.setStatus(SeatStatus.BOOKED);
                seat.setReservationId(null);
                eventSeatRepository.save(seat);
            }

            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            log.info("Successfully confirmed Order ID: {}", order.getId());

            // --- SEND KAFKA EVENT ---
            // Create payload based on the final confirmed state
            List<Long> eventSeatIds = seatsToBook.stream().map(EventSeat::getId).collect(Collectors.toList());
            OrderEventPayload kafkaPayload = OrderEventPayload.builder()
                    .orderId(order.getId())
                    .userId(order.getUser().getId()) // Get user ID from the order object
                    .reservationId(order.getReservationId())
                    .orderStatus(order.getStatus()) // Now CONFIRMED
                    .totalAmount(order.getTotalAmount())
                    .timestamp(order.getCreatedAt()) // Or update to confirmation time if needed
                    .eventSeatIds(eventSeatIds)
                    .build();

            // Send only to analytics topic upon final confirmation
            kafkaProducerService.sendAnalyticsEvent(kafkaPayload);
            // --- END KAFKA EVENT ---

        } catch (Exception e) {
            log.error("Failed to process booking confirmation for Order ID {}: {}", message.getOrderId(), e.getMessage(), e);

            // Optional: Send a Kafka event indicating processing failure
            if (order != null) { // Check if order was loaded
                List<Long> eventSeatIds = (seatsToBook != null) ? seatsToBook.stream().map(EventSeat::getId).collect(Collectors.toList()) : List.of();
                OrderEventPayload kafkaPayload = OrderEventPayload.builder()
                        .orderId(order.getId())
                        .userId(order.getUser().getId())
                        .reservationId(order.getReservationId())
                        .orderStatus(OrderStatus.FAILED) // Indicate failure
                        .totalAmount(order.getTotalAmount())
                        .timestamp(order.getCreatedAt())
                        .eventSeatIds(eventSeatIds) // Include seat IDs if available
                        .build();
                kafkaProducerService.sendAuditEvent(kafkaPayload); // Audit the failure
            }

            throw e; // Re-throw to trigger DLQ
        }
    }
}