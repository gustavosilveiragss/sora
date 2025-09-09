package com.sora.backend.dto;

public record UserSummaryDto(
    Long id,
    String username,
    String firstName,
    String lastName,
    String profilePicture,
    Integer countriesVisitedCount,
    Boolean isFollowedByCurrentUser
) {}