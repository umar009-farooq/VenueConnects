package com.venueconnect.venue;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(nullable = false)
    private String seatRow; // e.g., "A", "B", "Balcony 1"

    @Column(nullable = false)
    private String seatNumber; // e.g., "1", "2", "101"

    // Optional: A category for the seat, which can help determine pricing
    private String seatCategory; // e.g., "STANDARD", "PREMIUM"
}