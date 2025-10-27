package com.venueconnect.event.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class VenueResponse {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String country;
    private List<HallResponse> halls;
}