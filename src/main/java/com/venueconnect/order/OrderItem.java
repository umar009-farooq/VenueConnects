package com.venueconnect.order;

import com.venueconnect.event.EventSeat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Link to the specific seat that was booked
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_seat_id", nullable = false, unique = true)
    private EventSeat eventSeat;

    // Store the price at the time of purchase
    @Column(nullable = false)
    private BigDecimal price;
}