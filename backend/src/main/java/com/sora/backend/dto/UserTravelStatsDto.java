package com.sora.backend.dto;

public record UserTravelStatsDto(
    Integer countriesVisitedCount,
    Integer citiesVisitedCount,
    Integer totalPostsCount,
    Integer totalLikesReceived,
    Integer totalCommentsReceived
) {}