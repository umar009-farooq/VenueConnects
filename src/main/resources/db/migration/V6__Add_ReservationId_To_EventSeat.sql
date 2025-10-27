-- V6__Add_ReservationId_To_EventSeat.sql

-- Add a new column to store the temporary reservation ID
ALTER TABLE event_seats
ADD COLUMN reservation_id VARCHAR(255);

-- Add an index to this new column for fast lookups
CREATE INDEX idx_event_seats_reservation_id ON event_seats(reservation_id);