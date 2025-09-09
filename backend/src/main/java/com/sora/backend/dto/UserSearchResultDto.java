package com.sora.backend.dto;

import java.util.List;

public record UserSearchResultDto(
    Long id,
    String username,
    String firstName,
    String lastName,
    String bio,
    String profilePicture,
    Integer countriesVisitedCount,
    Integer followersCount,
    Boolean isFollowedByCurrentUser,
    List<String> commonCountries,
    LastActiveCountryDto lastActiveCountry
) {}
