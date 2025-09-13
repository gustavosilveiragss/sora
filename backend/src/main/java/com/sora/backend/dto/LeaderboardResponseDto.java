package com.sora.backend.dto;

import java.util.List;

public record LeaderboardResponseDto(
    String metric,
    String timeframe,
    Integer currentUserPosition,
    List<LeaderboardEntryDto> leaderboard
) {
    public record LeaderboardEntryDto(
        Integer position,
        UserSummaryDto user,
        Integer score,
        String scoreName,
        Boolean isCurrentUser
    ) {}
}