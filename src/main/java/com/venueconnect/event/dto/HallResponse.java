package com.venueconnect.event.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HallResponse {
    private Long id;
    private String name;
    private Integer capacity;
}