package com.venueconnect;


import com.venueconnect.config.RabbitMQConfig;
import com.venueconnect.event.*;
import com.venueconnect.kafka.KafkaProducerService;
import com.venueconnect.kafka.OrderEventPayload;
import com.venueconnect.order.*;
import com.venueconnect.reservation.Reservation;

import com.venueconnect.user.User;
import com.venueconnect.user.UserRepository;
import com.venueconnect.venue.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // Correct import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional; // Import Transactional

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat; // Use specific import
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;


class OrderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired private OrderService orderService;

    // Repositories
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private HallRepository hallRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketTypeRepository ticketTypeRepository;
    @Autowired private EventSeatRepository eventSeatRepository;
    @Autowired private OrderRepository orderRepository;

    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private RabbitTemplate rabbitTemplate;

    @MockBean private KafkaProducerService kafkaProducerService;

    private User testUser;
    private Event testEvent;
    private EventSeat reservedSeat1;
    private EventSeat reservedSeat2;
    private String testReservationId;
    private final String REDIS_KEY_PREFIX = "Reservation:";

    @BeforeEach
    @Sql("/sql/insert-test-user.sql") // Ensure user exists before setup
    void setUpTestData() {
        // Clean non-user data ONLY
        orderRepository.deleteAll();
        eventSeatRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        redisTemplate.delete(redisTemplate.keys("Reservation:*"));

        // Fetch the user created by @Sql
        testUser = userRepository.findByEmail("testuser@example.com").orElseThrow(() -> new IllegalStateException("Test user not found"));

        // Create Venue, Hall, Seats
        Venue venue = venueRepository.save(Venue.builder().name("Test Venue").build());
        Hall hall = hallRepository.save(Hall.builder().name("Test Hall").venue(venue).build());
        Seat seat1 = seatRepository.save(Seat.builder().hall(hall).seatRow("A").seatNumber("1").build());
        Seat seat2 = seatRepository.save(Seat.builder().hall(hall).seatRow("A").seatNumber("2").build());

        // Create Event, TicketType
        testEvent = eventRepository.save(Event.builder()
                .name("Test Event")
                .hall(hall)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .build());
        TicketType ticketType = ticketTypeRepository.save(TicketType.builder()
                .event(testEvent)
                .name("Standard")
                .price(BigDecimal.TEN)
                .totalQuantity(100)
                .build());

        // Simulate Reservation
        testReservationId = UUID.randomUUID().toString();
        reservedSeat1 = eventSeatRepository.save(EventSeat.builder()
                .event(testEvent).seat(seat1).ticketType(ticketType)
                .status(SeatStatus.RESERVED).reservationId(testReservationId).build());
        reservedSeat2 = eventSeatRepository.save(EventSeat.builder()
                .event(testEvent).seat(seat2).ticketType(ticketType)
                .status(SeatStatus.RESERVED).reservationId(testReservationId).build());
        Reservation reservation = Reservation.builder()
                .id(testReservationId).userId(testUser.getId()).eventId(testEvent.getId())
                .status("PENDING").createdAt(Instant.now()).build();
        String redisKey = REDIS_KEY_PREFIX + testReservationId;
        redisTemplate.opsForValue().set(redisKey, reservation, 15, TimeUnit.MINUTES);
    }

    @Test
    @Sql(scripts = "/sql/insert-test-user.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @WithUserDetails("testuser@example.com")
//    @Transactional // Keep session open for lazy loading
    void createOrder_shouldCreatePendingOrderPublishMessagesAndDeleteRedis_whenReservationIsValid() {
        // --- Arrange ---
        PaymentRequest request = new PaymentRequest(testReservationId, "MOCK_PAYMENT");

        // --- Act ---
        OrderResponse response = orderService.createOrder(request);

        // --- Assert ---

        // 1. Check response DTO (OrderService returns PAYMENT_COMPLETE initially)
        assertNotNull(response);
        assertNotNull(response.getOrderId());
        assertEquals(OrderStatus.PAYMENT_COMPLETE.name(), response.getStatus());
        assertEquals(0, new BigDecimal("20.00").compareTo(response.getTotalAmount()));
        assertThat(response.getItems()).hasSize(2);

        // Allow worker time to process the message
        try {
            System.out.println("Waiting for worker...");
            Thread.sleep(2000); // Wait 2 seconds
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // 2. Verify Order final state in Postgres DB (Worker should have updated it)
        Order savedOrder = orderRepository.findByIdWithItems(response.getOrderId()) // <-- CHANGE THIS LINE
                .orElseThrow(() -> new EntityNotFoundException("Order not found after test"));

        assertEquals(testUser.getId(), savedOrder.getUser().getId());
        assertEquals(OrderStatus.CONFIRMED, savedOrder.getStatus());
        assertEquals(testReservationId, savedOrder.getReservationId());
        assertEquals(0, new BigDecimal("20.00").compareTo(savedOrder.getTotalAmount()));
        // This assertion will now work because items were fetched eagerly
        assertThat(savedOrder.getOrderItems()).hasSize(2);
        // 3. Verify EventSeats final state in Postgres DB (Worker should have updated them)
        EventSeat seat1After = eventSeatRepository.findById(reservedSeat1.getId()).orElseThrow();
        EventSeat seat2After = eventSeatRepository.findById(reservedSeat2.getId()).orElseThrow();
        assertEquals(SeatStatus.BOOKED, seat1After.getStatus());    // Expect BOOKED
        assertNull(seat1After.getReservationId());                 // Expect reservationId cleared
        assertEquals(SeatStatus.BOOKED, seat2After.getStatus());    // Expect BOOKED
        assertNull(seat2After.getReservationId());                 // Expect reservationId cleared

        // 4. Verify Reservation deleted from Redis
        String redisKey = REDIS_KEY_PREFIX + testReservationId;
        Boolean keyExists = redisTemplate.hasKey(redisKey);
        assertEquals(false, keyExists, "Reservation key should be deleted from Redis");

        // 5. Verify RabbitMQ message was consumed (queue is empty)
        Long messageCount = rabbitTemplate.execute(channel -> channel.messageCount(RabbitMQConfig.BOOKING_QUEUE_NAME));
        assertNotNull(messageCount, "Could not get message count");
        assertEquals(0L, messageCount.longValue(), "Booking queue should be empty after worker processing");

        // 6. Verify Kafka messages sent (using Mockito)
        // Verify OrderService sent PAYMENT_COMPLETE events
        verify(kafkaProducerService, timeout(1000).times(1)).sendAuditEvent(
                argThat(payload -> payload instanceof OrderEventPayload &&
                        ((OrderEventPayload) payload).getOrderId().equals(savedOrder.getId()) &&
                        ((OrderEventPayload) payload).getOrderStatus() == OrderStatus.PAYMENT_COMPLETE)
        );
        verify(kafkaProducerService, timeout(1000).times(1)).sendAnalyticsEvent(
                argThat(payload -> payload instanceof OrderEventPayload &&
                        ((OrderEventPayload) payload).getOrderId().equals(savedOrder.getId()) &&
                        ((OrderEventPayload) payload).getOrderStatus() == OrderStatus.PAYMENT_COMPLETE)
        );
        // Verify BookingWorker sent CONFIRMED analytics event
        verify(kafkaProducerService, timeout(3000).times(1)).sendAnalyticsEvent( // Slightly longer timeout
                argThat(payload -> payload instanceof OrderEventPayload &&
                        ((OrderEventPayload) payload).getOrderId().equals(savedOrder.getId()) &&
                        ((OrderEventPayload) payload).getOrderStatus() == OrderStatus.CONFIRMED)
        );
    }
}