package com.sora.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserStatsDto(
    Long totalCountriesVisited,
    Long totalCitiesVisited,
    Long totalPostsCount,
    Long totalLikesReceived,
    Long totalCommentsReceived,
    Long totalFollowers,
    LocalDate joinedDate,
    Long daysTraveling,
    Double averagePostsPerCountry
) {}