-- V2__Create_Venue_Event_Tables.sql

-- 1. Venues Table
CREATE TABLE venues (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100)
);

-- 2. Halls Table
CREATE TABLE halls (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    capacity INT,
    venue_id BIGINT NOT NULL,
    CONSTRAINT fk_halls_venue
        FOREIGN KEY(venue_id)
        REFERENCES venues(id)
        ON DELETE CASCADE -- If a venue is deleted, delete its halls
);

-- 3. Events Table
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    hall_id BIGINT NOT NULL,
    CONSTRAINT fk_events_hall
        FOREIGN KEY(hall_id)
        REFERENCES halls(id)
        -- We don't cascade delete here, to prevent accidental event deletion
);

-- 4. Ticket Types Table
CREATE TABLE ticket_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    total_quantity INT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT fk_ticket_types_event
        FOREIGN KEY(event_id)
        REFERENCES events(id)
        ON DELETE CASCADE -- If an event is deleted, delete its ticket types
);

-- Add indexes for better query performance on foreign keys
CREATE INDEX idx_halls_venue_id ON halls(venue_id);
CREATE INDEX idx_events_hall_id ON events(hall_id);
CREATE INDEX idx_events_start_time ON events(start_time);
CREATE INDEX idx_ticket_types_event_id ON ticket_types(event_id);