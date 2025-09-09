package com.sora.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UserProfileDto(
    Long id,
    String username,
    String email,
    String firstName,
    String lastName,
    String bio,
    String profilePicture,
    Boolean isActive,
    Integer countriesVisitedCount,
    Integer citiesVisitedCount,
    Integer followersCount,
    Integer followingCount,
    Integer totalPostsCount,
    Boolean isFollowedByCurrentUser,
    LocalDateTime joinedAt,
    LocalDateTime lastActiveAt,
    List<LastActiveCountryDto> lastActiveCountries
) {}