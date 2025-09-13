package com.sora.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GlobeDataResponseDto(
    String globeType,
    Integer totalCountriesWithActivity,
    Integer totalRecentPosts,
    LocalDateTime lastUpdated,
    List<CountryMarkerDto> countryMarkers
) {}