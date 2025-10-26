package com.sora.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record UserGamificationStatsResponseDto(
    UserSummaryDto user,
    TravelStatsDto travelStats,
    RankingsDto rankings,
    List<AchievementDto> achievements,
    List<ContinentStatsDto> continentStats
) {
    public record TravelStatsDto(
        Integer totalCountriesVisited,
        Integer totalCitiesVisited,
        Integer totalPostsCount,
        Integer totalLikesReceived,
        Integer totalCommentsReceived,
        Integer totalFollowers,
        Integer totalFollowing,
        LocalDate joinedDate,
        Integer daysTraveling,
        Double averagePostsPerCountry
    ) {}

    public record RankingsDto(
        RankingPositionDto countriesRankAmongMutuals,
        RankingPositionDto postsRankAmongMutuals
    ) {}

    public record RankingPositionDto(
        Integer position,
        Integer totalUsers,
        Double percentile
    ) {}

    public record AchievementDto(
        String code,
        String nameKey,
        String descriptionKey,
        String iconName,
        LocalDate unlockedAt,
        String requirements
    ) {}

    public record ContinentStatsDto(
        String continentCode,
        String continentNameKey,
        Integer countriesVisited,
        Integer totalCountries,
        Double completionPercentage,
        Integer postsCount
    ) {}
}