package com.venueconnect.order;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    private String reservationId;
    private String paymentMethod; // e.g., "MOCK_GATEWAY", "CREDIT_CARD"
}