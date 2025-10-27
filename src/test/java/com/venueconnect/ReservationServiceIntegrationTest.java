package com.venueconnect;

import com.venueconnect.event.*;
import com.venueconnect.reservation.Reservation;
import com.venueconnect.reservation.ReservationRequest;
import com.venueconnect.reservation.ReservationResponse;
import com.venueconnect.reservation.ReservationService;
import com.venueconnect.user.Role;
import com.venueconnect.user.User;
import com.venueconnect.user.UserRepository;
import com.venueconnect.venue.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat; // Use AssertJ for assertions
import static org.junit.jupiter.api.Assertions.*;

// Extend the base class to inherit container setup and property overrides
class ReservationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired // Inject the service we want to test
    private ReservationService reservationService;

    // Inject repositories needed for setup and verification
    @Autowired private UserRepository userRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private HallRepository hallRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private TicketTypeRepository ticketTypeRepository;
    @Autowired private EventSeatRepository eventSeatRepository;

    @Autowired // Inject RedisTemplate for verification
    private RedisTemplate<String, Object> redisTemplate;

    private User testUser;
    private Event testEvent;
    private EventSeat availableSeat1;
    private EventSeat availableSeat2;

    // --- Test Data Setup ---
    @BeforeEach // Runs before each test method
    void setUpTestData() {
        // Clean up potentially existing data from previous tests (important!)
        // Order matters due to foreign keys
        eventSeatRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        seatRepository.deleteAll();
        hallRepository.deleteAll();
        venueRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User
        testUser = User.builder()
                .email("testuser@example.com")
                .password("password") // Password doesn't matter for @WithMockUser
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(testUser);

        // 2. Create Venue, Hall, Seat
        Venue venue = venueRepository.save(Venue.builder().name("Test Venue").build());
        Hall hall = hallRepository.save(Hall.builder().name("Test Hall").venue(venue).build());
        Seat seat1 = seatRepository.save(Seat.builder().hall(hall).seatRow("A").seatNumber("1").build());
        Seat seat2 = seatRepository.save(Seat.builder().hall(hall).seatRow("A").seatNumber("2").build());

        // 3. Create Event, TicketType
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

        // 4. Create EventSeats (the inventory)
        availableSeat1 = eventSeatRepository.save(EventSeat.builder()
                .event(testEvent)
                .seat(seat1)
                .ticketType(ticketType)
                .status(SeatStatus.AVAILABLE)
                .build());
        availableSeat2 = eventSeatRepository.save(EventSeat.builder()
                .event(testEvent)
                .seat(seat2)
                .ticketType(ticketType)
                .status(SeatStatus.AVAILABLE)
                .build());
    }

    // --- Test Method ---

    @Test
//    @WithMockUser(username = "testuser@example.com", roles = "USER") // Simulate user login
    @Sql("/sql/insert-test-user.sql")
    @WithUserDetails("testuser@example.com")
    void createReservation_shouldReserveSeatsAndCreateRedisEntry_whenSeatsAreAvailable() {
        // --- Arrange ---
        ReservationRequest request = new ReservationRequest(
                testEvent.getId(),
                List.of(availableSeat1.getId(), availableSeat2.getId())
        );

        // --- Act ---
        ReservationResponse response = reservationService.createReservation(request);

        // --- Assert ---

        // 1. Check the response DTO
        assertNotNull(response);
        assertNotNull(response.getReservationId());
        assertEquals(testEvent.getId(), response.getEventId());
        assertEquals("PENDING", response.getStatus());
        assertThat(response.getEventSeatIds()).containsExactlyInAnyOrder(availableSeat1.getId(), availableSeat2.getId());
        assertNotNull(response.getExpiresAt());

        // 2. Verify seats in Postgres DB
        EventSeat seat1After = eventSeatRepository.findById(availableSeat1.getId()).orElseThrow();
        EventSeat seat2After = eventSeatRepository.findById(availableSeat2.getId()).orElseThrow();

        assertEquals(SeatStatus.RESERVED, seat1After.getStatus());
        assertEquals(response.getReservationId(), seat1After.getReservationId());
        assertEquals(SeatStatus.RESERVED, seat2After.getStatus());
        assertEquals(response.getReservationId(), seat2After.getReservationId());

        // 3. Verify reservation in Redis DB
        String redisKey = "Reservation:" + response.getReservationId();
        Reservation savedReservation = (Reservation) redisTemplate.opsForValue().get(redisKey);

        assertNotNull(savedReservation);
        assertEquals(response.getReservationId(), savedReservation.getId());
        assertEquals(testUser.getId(), savedReservation.getUserId());
        assertEquals(testEvent.getId(), savedReservation.getEventId());
        assertEquals("PENDING", savedReservation.getStatus());
        assertNotNull(savedReservation.getCreatedAt());

        // 4. Verify TTL in Redis (check if it's roughly 15 minutes)
        // 4. Verify TTL in Redis (check if it's roughly 15 minutes)
        Long ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS); // Get TTL in SECONDS
        System.out.println("### DEBUG: TTL for key " + redisKey + " is " + ttlSeconds + " seconds."); // Add logging

        assertNotNull(ttlSeconds, "TTL should not be null"); // Check if TTL exists

        // Convert expected TTL to seconds
        long expectedTtlSeconds = TimeUnit.MINUTES.toSeconds(ReservationService.RESERVATION_TTL_MINUTES);

        // Check if the TTL is within a reasonable range (e.g., within 5 seconds of expected)
        assertTrue(ttlSeconds <= expectedTtlSeconds, "TTL should be less than or equal to expected");
        assertTrue(ttlSeconds > expectedTtlSeconds - 5, "TTL should be close to expected (within 5 seconds)"); // Check lower bound
    }

    // --- TODO: Add more tests ---
    // - Test reserving already reserved seat (should fail)
    // - Test reserving seat for wrong event (should fail)
    // - Test reserving non-existent seat (should fail)
}