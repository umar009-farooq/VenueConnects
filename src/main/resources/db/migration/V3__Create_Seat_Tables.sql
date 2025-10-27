-- V3__Create_Seat_Tables.sql

-- 1. Seats Table (Physical seats in a hall)
CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    hall_id BIGINT NOT NULL,
    seat_row VARCHAR(50) NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    seat_category VARCHAR(100),

    CONSTRAINT fk_seats_hall
        FOREIGN KEY(hall_id)
        REFERENCES halls(id)
        ON DELETE CASCADE, -- If a hall is deleted, its seats are deleted

    -- A seat must be unique within its hall
    CONSTRAINT uq_hall_seat UNIQUE(hall_id, seat_row, seat_number)
);

-- 2. Event Seats Table (The status of a seat for a specific event)
CREATE TABLE event_seats (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    ticket_type_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL, -- (AVAILABLE, RESERVED, BOOKED)

    CONSTRAINT fk_event_seats_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE, -- If an event is deleted, delete its seat statuses

    CONSTRAINT fk_event_seats_seat
        FOREIGN KEY(seat_id)
        REFERENCES seats(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_event_seats_ticket_type
        FOREIGN KEY(ticket_type_id)
        REFERENCES ticket_types(id)
        ON DELETE NO ACTION, -- Don't cascade delete if a ticket type is removed

    -- A seat can only be listed once per event
    CONSTRAINT uq_event_seat UNIQUE(event_id, seat_id)
);

-- Add indexes for fast lookups
CREATE INDEX idx_seats_hall_id ON seats(hall_id);
CREATE INDEX idx_event_seats_event_id ON event_seats(event_id);
CREATE INDEX idx_event_seats_seat_id ON event_seats(seat_id);