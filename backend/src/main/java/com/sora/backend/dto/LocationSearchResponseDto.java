package com.sora.backend.dto;

import java.util.List;

public record LocationSearchResponseDto(
    String query,
    String countryFilter,
    List<LocationResultDto> results
) {
    public record LocationResultDto(
        String displayName,
        String cityName,
        String stateName,
        String countryCode,
        String countryName,
        Double latitude,
        Double longitude,
        Double importance,
        String osmType
    ) {}
}