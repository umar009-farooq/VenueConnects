package com.venueconnect.order;

public enum OrderStatus {
    PENDING,    // Order created, waiting for payment/confirmation
    CONFIRMED,  // Payment successful, seats are booked
    FAILED,     // Payment failed
    CANCELLED,   // Order was cancelled
    PAYMENT_COMPLETE
}