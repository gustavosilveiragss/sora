package com.sora.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RecentDestinationsResponseDto(
    Long userId,
    String username,
    List<RecentDestinationDto> recentDestinations
) {
    public record RecentDestinationDto(
        CountryDto country,
        String lastCityVisited,
        LocalDateTime lastPostDate,
        Integer recentPostsCount
    ) {}
}