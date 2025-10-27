package com.venueconnect.venue;

import com.venueconnect.event.dto.HallResponse;
import com.venueconnect.event.dto.VenueResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToVenueResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VenueResponse getVenue(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + id));
        return mapToVenueResponse(venue);
    }

    // Helper method to map Venue Entity to VenueResponse DTO
    private VenueResponse mapToVenueResponse(Venue venue) {
        List<HallResponse> hallResponses = venue.getHalls().stream()
                .map(hall -> HallResponse.builder()
                        .id(hall.getId())
                        .name(hall.getName())
                        .capacity(hall.getCapacity())
                        .build())
                .collect(Collectors.toList());

        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .country(venue.getCountry())
                .halls(hallResponses)
                .build();
    }
}