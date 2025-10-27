-- V4__Create_Order_Tables.sql

-- 1. Orders Table
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- (PENDING, CONFIRMED, FAILED, CANCELLED)
    created_at TIMESTAMP NOT NULL,
    reservation_id VARCHAR(255) UNIQUE, -- Link to the Redis reservation

    CONSTRAINT fk_orders_user
        FOREIGN KEY(user_id)
        REFERENCES users(id)
        ON DELETE NO ACTION -- Don't delete orders if a user is deleted
);

-- 2. Order Items Table
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_seat_id BIGINT NOT NULL UNIQUE, -- A seat can only be in one order item
    price DECIMAL(10, 2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY(order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE, -- If an order is deleted, delete its items

    CONSTRAINT fk_order_items_event_seat
        FOREIGN KEY(event_seat_id)
        REFERENCES event_seats(id)
        ON DELETE NO ACTION -- Don't delete order items if a seat is deleted
);

-- Add indexes for fast lookups
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_reservation_id ON orders(reservation_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);