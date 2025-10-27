-- V5__Insert_Test_Data.sql
-- This script inserts a complete set of test data for a single event.

-- 1. Create a Venue
INSERT INTO venues(id, name, address, city, country)
VALUES (1, 'Grand Theater', '123 Main St', 'New York', 'USA')
ON CONFLICT (id) DO NOTHING; -- 'ON CONFLICT' prevents errors if you re-run this

-- 2. Create a Hall in that Venue
INSERT INTO halls(id, name, capacity, venue_id)
VALUES (1, 'Main Auditorium', 4, 1)
ON CONFLICT (id) DO NOTHING;

-- 3. Create 4 physical Seats in that Hall
INSERT INTO seats(id, hall_id, seat_row, seat_number, seat_category)
VALUES
    (1, 1, 'A', '1', 'STANDARD'),
    (2, 1, 'A', '2', 'STANDARD'),
    (3, 1, 'B', '1', 'VIP'),
    (4, 1, 'B', '2', 'VIP')
ON CONFLICT (id) DO NOTHING;

-- 4. Create an Event
INSERT INTO events(id, name, description, start_time, end_time, hall_id)
VALUES (1, 'Rockstars Live', 'A live concert featuring the biggest rockstars.', '2025-12-01 20:00:00', '2025-12-01 22:00:00', 1)
ON CONFLICT (id) DO NOTHING;

-- 5. Create Ticket Types for this Event
INSERT INTO ticket_types(id, name, price, total_quantity, event_id)
VALUES
    (1, 'Standard Admission', 50.00, 2, 1), -- 2 standard seats
    (2, 'VIP Package', 150.00, 2, 1)      -- 2 VIP seats
ON CONFLICT (id) DO NOTHING;

-- 6. Create the EventSeats (The most important part!)
-- This links the physical seats to the event and gives them a price/status.
INSERT INTO event_seats(id, event_id, seat_id, ticket_type_id, status)
VALUES
    -- Link Standard seats to Standard ticket price
    (1, 1, 1, 1, 'AVAILABLE'), -- Event 1, Seat A1, Standard Price, AVAILABLE
    (2, 1, 2, 1, 'AVAILABLE'), -- Event 1, Seat A2, Standard Price, AVAILABLE

    -- Link VIP seats to VIP ticket price
    (3, 1, 3, 2, 'AVAILABLE'), -- Event 1, Seat B1, VIP Price, AVAILABLE
    (4, 1, 4, 2, 'AVAILABLE')  -- Event 1, Seat B2, VIP Price, AVAILABLE
ON CONFLICT (id) DO NOTHING;

-- This is necessary to reset the auto-increment counters so new inserts work properly
SELECT setval('venues_id_seq', (SELECT MAX(id) FROM venues));
SELECT setval('halls_id_seq', (SELECT MAX(id) FROM halls));
SELECT setval('seats_id_seq', (SELECT MAX(id) FROM seats));
SELECT setval('events_id_seq', (SELECT MAX(id) FROM events));
SELECT setval('ticket_types_id_seq', (SELECT MAX(id) FROM ticket_types));
SELECT setval('event_seats_id_seq', (SELECT MAX(id) FROM event_seats));